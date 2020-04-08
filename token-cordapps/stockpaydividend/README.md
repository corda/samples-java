# Stock Pay Dividend

This CorDapp aims to demonstrate the usage of TokenSDK, especially the concept of EvolvableToken which represents stock.
You will find the StockState extends from EvolvableToken which allows the stock details(eg. announcing dividends) to be updated without affecting the parties who own the stock.

* [Concepts](#concepts)
* [Usage](#usage)

## Concepts


### Parties

This CordApp assumes there are 4 parties:

* **WayneCo** - creates and maintains the stock state and pays dividends to shareholders after some time passes.
* **Shareholder** - receives dividends base on the owning stock.
* **Bank** - issues fiat tokens.
* **Observer** - monitors all the stocks by keeping a copy of of transactions whenever a stock is created or updated. (In reality, this might be a financial regulatory authority like the SEC.)


Here's the flows that exist between these parties :

![Flow diagram](diagrams/FlowDiagram2.png)


This Stock Exchange CorDapp includes:
* A bank issues some money for the final settlement of the dividends.
* A company/stock issuer(WayneCo) issues and moves stocks to shareholders
* The company announces dividends for shareholders to claim before execution day
* Shareholder retrieves the most updated stock information and then claims dividend
* The company distribute dividends to shareholders


### Keys to learn
* Basic usage of TokenSDK
* How the state of stock (ie. EvolvableToken) updates independently without stock holders involved
* Use of `TokenSelection.generateMove()` and `MoveTokensUtilitiesKt.addMoveTokens()` to generate move of tokens
* Adding observers in token transactions with TokenSDK

*Note that some date constraint(eg. payday) is being commented out to make sure the sample can be ran smoothly

### States
* **[StockState](contracts/src/main/java/net/corda/examples/stockpaydividend/states/StockState.java)** -
which holds the underlying information of a stock like stock name, symbol, dividend, etc.
* **[DividendState](contracts/src/main/java/net/corda/examples/stockpaydividend/states/DividendState.java)** -
represents the dividend to be paid off by the company to the shareholder.


### Flows

We'll list the flows here in the order that they execute in our example.


##### Pre-requisite. IssueMoney - Bank

First, the bank issues money to WayneCo using [IssueMoney.java](https://github.com/corda/samples-java/blob/master/token-cordapps/stockpaydividend/workflows/src/main/java/net/corda/examples/stockpaydividend/flows/IssueMoney.java#L37-L2)

```java
public String call() throws FlowException {

    // Create an instance of the fiat currency token type
    TokenType token = FiatCurrency.Companion.getInstance(currency);

    // Create an instance of IssuedTokenType which represents the token is issued by this party
    IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), token);

    // Create an instance of FungibleToken for the fiat currency to be issued
    FungibleToken fungibleToken = new FungibleToken(new Amount<>(quantity, issuedTokenType), recipient, null);

    // Use the build-in flow, IssueTokens, to issue the required amount to the the recipient
    SignedTransaction stx = subFlow(new IssueTokens(ImmutableList.of(fungibleToken), ImmutableList.of(recipient)));
    return "\nIssued to " + recipient.getName().getOrganisation() + " " + this.quantity +" "
            + this.currency +" for stock issuance."+ "\nTransaction ID: "+ stx.getId();
}
```


##### 1. IssueStock - Stock Issuer
WayneCo creates a StockState and issues some stock tokens associated to the created StockState.

That stock is issused in [IssueStock.java](https://github.com/corda/samples-java/blob/master/token-cordapps/stockpaydividend/workflows/src/main/java/net/corda/examples/stockpaydividend/flows/IssueStock.java#L57-L97).

```java
public String call() throws FlowException {

    // Sample specific - retrieving the hard-coded observers
    IdentityService identityService = getServiceHub().getIdentityService();
    List<Party> observers = ObserversUtilities.getObserverLegalIdenties(identityService);

    Party company = getOurIdentity();

    // Construct the output StockState
    final StockState stockState = new StockState(
            new UniqueIdentifier(),
            company,
            symbol,
            name,
            currency,
            price,
            BigDecimal.valueOf(0), // A newly issued stock should not have any dividend
            new Date(),
            new Date()
    );

    // The notary provided here will be used in all future actions of this token
    TransactionState<StockState> transactionState = new TransactionState<>(stockState, notary);

    // Using the build-in flow to create an evolvable token type -- Stock
    subFlow(new CreateEvolvableTokens(transactionState, observers));

    // Similar in IssueMoney flow, class of IssuedTokenType represents the stock is issued by the company party
    IssuedTokenType issuedStock = new IssuedTokenType(company, stockState.toPointer(stockState.getClass()));

    // Create an specified amount of stock with a pointer that refers to the StockState
    Amount<IssuedTokenType> issueAmount = new Amount(new Long(issueVol), issuedStock);

    // Indicate the recipient which is the issuing party itself here
    FungibleToken stockToken = new FungibleToken(issueAmount, getOurIdentity(), null);

    // Finally, use the build-in flow to issue the stock tokens. Observer parties provided here will record a copy of the transactions
    SignedTransaction stx = subFlow(new IssueTokens(ImmutableList.of(stockToken), observers));
    return "\nGenerated " + this.issueVol + " " + this.symbol + " stocks with price: "
            + this.price + " " + this.currency + "\nTransaction ID: "+ stx.getId();
}
```


##### 2. MoveStock - Stock Issuer

WayneCo transfers some stock tokens to the Shareholder.


This flow is in [MoveStock.java](https://github.com/corda/samples-java/blob/master/token-cordapps/stockpaydividend/workflows/src/main/java/net/corda/examples/stockpaydividend/flows/MoveStock.java#L40-L53)

```java
public String call() throws FlowException {

    // To get the transferring stock, we can get the StockState from the vault and get it's pointer
    TokenPointer<StockState> stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());

    // With the pointer, we can get the create an instance of transferring Amount
    Amount<TokenType> amount = new Amount(quantity, stockPointer);

    //Use built-in flow for move tokens to the recipient
    SignedTransaction stx = subFlow(new MoveFungibleTokens(amount, recipient));
    return "\nIssued "+this.quantity +" " +this.symbol+" stocks to "
            + this.recipient.getName().getOrganisation() + ".\nTransaction ID: "+stx.getId();

}
```


##### 3. AnnounceDividend - Stock Issuer
WayneCo announces the dividends that will be paid on the payday.

This happens through [AnnounceDividend.java](https://github.com/corda/samples-java/blob/master/token-cordapps/stockpaydividend/workflows/src/main/java/net/corda/examples/stockpaydividend/flows/AnnounceDividend.java#L48-L77)

```java
public String call() throws FlowException {

    // Retrieved the unconsumed StockState from the vault
    StateAndRef<StockState> stockStateRef = QueryUtilities.queryStock(symbol, getServiceHub());
    StockState stock = stockStateRef.getState().getData();

    // Form the output state here with a dividend to be announced
    StockState outputState = new StockState(
            stock.getLinearId(),
            stock.getIssuer(),
            stock.getSymbol(),
            stock.getName(),
            stock.getCurrency(),
            stock.getPrice(),
            dividendPercentage,
            executionDate,
            payDate);

    // Get predefined observers
    IdentityService identityService = getServiceHub().getIdentityService();
    List<Party> observers = ObserversUtilities.getObserverLegalIdenties(identityService);
    List<FlowSession> obSessions = new ArrayList<>();
    for(Party observer : observers){
        obSessions.add(initiateFlow(observer));
    }

    // Update the stock state and send a copy to the observers eventually
    subFlow(new UpdateEvolvableTokenFlow(stockStateRef, outputState, ImmutableList.of(), obSessions));
    return "\nStock " + this.symbol + " has changed dividend percentage to " + this.dividendPercentage + ". \n";
}
```



##### 4. GetStockUpdate - Shareholder
Shareholders retrieves the newest stock state from the company.


We see this happen in [GetStockUpdate.java](https://github.com/corda/samples-java/blob/master/token-cordapps/stockpaydividend/workflows/src/main/java/net/corda/examples/stockpaydividend/flows/GetStockUpdate.java#L33-L52)

```java
public String call() throws FlowException {

    // Retrieve the most updated and unconsumed StockState and get it's pointer
    // This may be redundant as stock company will query the vault again.
    // But the point is to make sure this node owns this stock as company is considered as an busy node.
    TokenPointer stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());
    StockState stockState = (StockState) stockPointer.getPointer().resolve(getServiceHub()).getState().getData();

    // Send the stock symbol to the company to request for an update.
    FlowSession session = initiateFlow(stockState.getIssuer());
    session.send(stockState.getSymbol());

    // Receive the transaction, checks for the signatures of the state and then record it in vault
    // Note: Instead of ONLY_RELEVANT, ALL_VISIBLE is used here as the shareholder of the StockState is not a participant by the design of this CordApp
    SignedTransaction stx = subFlow(new ReceiveTransactionFlow(session, true, StatesToRecord.ALL_VISIBLE));

    StockState updatedState = getServiceHub().getVaultService().queryBy(StockState.class).getStates().get(0).getState().getData();

    return "\nThe current dividend is: "+ updatedState.getDividend()+ ".";
}
```


##### 5. ClaimDividendReceivable - Shareholder

Shareholders finds the dividend is announced and claims the dividends base on the owning stock.

Implemented in [ClaimDividendReceivable.java](https://github.com/corda/samples-java/blob/master/token-cordapps/stockpaydividend/workflows/src/main/java/net/corda/examples/stockpaydividend/flows/ClaimDividendReceivable.java#L51-L96)

```java
public String call() throws FlowException {

    // Retrieve the stock and pointer
    TokenPointer stockPointer = QueryUtilities.queryStockPointer(symbol, getServiceHub());
    StateAndRef<StockState> stockStateRef = stockPointer.getPointer().resolve(getServiceHub());
    StockState stockState = stockStateRef.getState().getData();

    // Query the current Stock amount from shareholder
    Amount<TokenType> stockAmount = QueryUtilitiesKt.tokenBalance(getServiceHub().getVaultService(), stockPointer);

    // Prepare to send the stock amount to the company to request dividend issuance
    ClaimNotification stockToClaim = new ClaimNotification(stockAmount);

    FlowSession session = initiateFlow(stockState.getIssuer());

    // First send the stock state as which stock state the shareholder is referring to
    subFlow(new SendStateAndRefFlow(session, ImmutableList.of(stockStateRef)));

    // Then send the stock amount
    session.send(stockToClaim);

    // Wait for the transaction from the company, and sign it after the checking
    class SignTxFlow extends SignTransactionFlow {
        private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
            super(otherPartyFlow, progressTracker);
        }

        @Override
        protected void checkTransaction(SignedTransaction stx) throws FlowException {
            requireThat(req -> {
                // Any checkings that the DividendContract is be able to validate. Below are some example constraints
                DividendState dividend = stx.getTx().outputsOfType(DividendState.class).get(0);
                req.using("Claimed dividend should be owned by Shareholder", dividend.getHolder().equals(getOurIdentity()));

                return null;
            });

        }
    }
    final SignTxFlow signTxFlow = new SignTxFlow(session, SignTransactionFlow.Companion.tracker());

    // Checks if the later transaction ID of the received FinalityFlow is the same as the one just signed
    final SecureHash txId = subFlow(signTxFlow).getId();
    subFlow(new ReceiveFinalityFlow(session, txId));
    return "\nRequest has been sent, Please wait for the stock issuer to respond.";
}
```


##### 6. PayDividend - Company
On the payday, the company pay off the stock with fiat currencies.


This is implemented in [PayDividend.java](https://github.com/corda/samples-java/blob/master/token-cordapps/stockpaydividend/workflows/src/main/java/net/corda/examples/stockpaydividend/flows/PayDividend.java#L46-L108).

```java
public List<String> call() throws FlowException {

    //Query the vault for any unconsumed DividendState
    List<StateAndRef<DividendState>> stateAndRefs = getServiceHub().getVaultService().queryBy(DividendState.class).getStates();

    List<SignedTransaction> transactions = new ArrayList<>();
    List<String> notes = new ArrayList<>();

    //For each queried unpaid DividendState, pay off the dividend with the corresponding amount.
    for(StateAndRef<DividendState> result : stateAndRefs){
        DividendState dividendState =  result.getState().getData();
        Party shareholder = dividendState.getHolder();

        // The amount of fiat tokens to be sent to the shareholder.
        PartyAndAmount<TokenType> sendingPartyAndAmount = new PartyAndAmount<>(shareholder, dividendState.getDividendAmount());

        // Instantiating an instance of TokenSelection which helps retrieving required tokens easily
        TokenSelection tokenSelection = TempTokenSelectionFactory.getTokenSelection(getServiceHub());

        // Generate input and output pair of moving fungible tokens
        Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> fiatIoPair = tokenSelection.generateMove(
                        getRunId().getUuid(),
                        ImmutableList.of(sendingPartyAndAmount),
                        getOurIdentity(),
                        null);

        // Using the notary from the previous transaction (dividend issuance)
        Party notary = result.getState().getNotary();

        // Create the required signers and the command
        List<PublicKey> requiredSigners = dividendState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
        Command payCommand = new Command(new DividendContract.Commands.Pay(), requiredSigners);

        // Start building transaction
        TransactionBuilder txBuilder = new TransactionBuilder(notary);
        txBuilder
                .addInputState(result)
                .addCommand(payCommand);
        // As a later part of TokenSelection.generateMove which generates a move of tokens handily
        MoveTokensUtilitiesKt.addMoveTokens(txBuilder, fiatIoPair.getFirst(), fiatIoPair.getSecond());

        // Verify the transactions with contracts
        txBuilder.verify(getServiceHub());

        // Sign the transaction
        SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder, getOurIdentity().getOwningKey());

        // Instantiate a network session with the shareholder
        FlowSession holderSession = initiateFlow(shareholder);

        final ImmutableSet<FlowSession> sessions = ImmutableSet.of(holderSession);

        // Ask the shareholder to sign the transaction
        final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                ptx,
                ImmutableSet.of(holderSession)));
        SignedTransaction fstx = subFlow(new FinalityFlow(stx, sessions));
        notes.add("\nPaid to " + dividendState.getHolder().getName().getOrganisation()
                + " " + (dividendState.getDividendAmount().getQuantity()/100) +" "
                + dividendState.getDividendAmount().getToken().getTokenIdentifier() + "\nTransaction ID: " + fstx.getId() );
    }
    return notes;
}
```


##### 7. Get token balances - Any node
Query the balances of different nodes. This can be executed at anytime.

This is found in two different flows, where we make requests using `GetStockBalances` or `GetFiatBalances` for stock or fiat with [QueryStock.java](https://github.com/corda/samples-java/blob/5c0155784e8ca9df27dd5d4f9eca65e4cce4b11a/token-cordapps/stockpaydividend/workflows/src/main/java/net/corda/examples/stockpaydividend/flows/QueryStock.java)


## Usage

### Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.


### Running the sample
To go through the sample flow, execute the commands on the corresponding node

##### Pre-requisite. IssueMoney - Bank
In order to pay off dividends from the company later, the bank issues some fiat tokens to the WayneCo.
This can be executed anytime before step 6.
>On bank node, execute <br>`start IssueMoney currency: USD, amount: 500000, recipient: WayneCo`

##### 1. IssueStock - Stock Issuer
WayneCo creates a StockState and issues some stock tokens associated to the created StockState.
>On company WayneCo's node, execute <br>`start IssueStock symbol: TEST, name: "Stock, SP500", currency: USD, price: 7.4, issueVol: 500, notary: Notary`

##### 2. MoveStock - Stock Issuer
WayneCo transfers some stock tokens to the Shareholder.
>On company WayneCo's node, execute <br>`start MoveStock symbol: TEST, quantity: 100, recipient: Shareholder`

Now at the Shareholder's terminal, we can see that it received 100 stock tokens:
>On shareholder node, execute <br>`start GetStockBalance symbol: TEST`

##### 3. AnnounceDividend - Stock Issuer
WayneCo announces the dividends that will be paid on the payday.
>On WayneCo's node, execute <br>`start AnnounceDividend symbol: TEST, dividendPercentage: 0.05, executionDate: "2019-11-22T00:00:00Z", payDate: "2019-11-23T00:00:00Z"`

##### 4. GetStockUpdate - Shareholder
Shareholders retrieves the newest stock state from the company.
>On shareholder node, execute <br>`start GetStockUpdate symbol: TEST`

##### 5. ClaimDividendReceivable - Shareholder
Shareholders finds the dividend is announced and claims the dividends base on the owning stock.
>On shareholder node, execute <br>`start ClaimDividendReceivable symbol: TEST`

##### 6. PayDividend - Company
On the payday, the company pay off the stock with fiat currencies.
>On WayneCo node, execute <br>`start PayDividend`

##### 7. Get token balances - Any node
Query the balances of different nodes. This can be executed at anytime.
> Get stock token balances
<br>`start GetStockBalance symbol: TEST`

>Get fiat token balances
<br>`start GetFiatBalance currencyCode: USD`

#### Test cases
You can also find the flow and example data from the test class [FlowTests.java](workflows/src/test/java/net/corda/examples/stockpaydividend/FlowTests.java).




## Useful links

### Documentation
[Token-SDK tutorial](https://github.com/corda/token-sdk/blob/master/docs/DvPTutorial.md)
<br>
[Token-SDK design document](https://github.com/corda/token-sdk/blob/95b7bac668c68f3108bca2c50f4f926d147ee763/design/design.md#evolvabletokentype)

### Other materials
[Blog - House trading sample](https://medium.com/corda/lets-create-some-tokens-5e7f94c39d13) -
A less complicated sample of TokenSDK about trading house.
<br>
[Blog - Introduction to Token SDK in Corda](https://medium.com/corda/introduction-to-token-sdk-in-corda-9b4dbcf71025) -
Provides basic understanding from the ground up.
<br>
[Sample - TokenSDK with Account](https://github.com/corda/accounts/tree/master/examples/tokens-integration-test)
An basic sample of how account feature can be integrated with TokenSDK

