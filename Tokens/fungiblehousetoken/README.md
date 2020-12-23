# Fungible House token sample CorDapp

This CorDapp serves as a basic example to create, issue, and move [Fungible](https://training.corda.net/libraries/tokens-sdk/#fungibletoken) tokens in Corda utilizing the Token SDK. In this specific fungible token sample, we will not talk about the redeem method of the Token SDK because the redeem process will take the physical asset off the ledger and destroy the token. Thus, this sample will be a simple walk though of the creation, issuance, and transfer of the tokens.

Quick blog about TokenSDK see [here](https://medium.com/corda/introduction-to-token-sdk-in-corda-9b4dbcf71025)


## Concepts


### Flows

There are a few flows that enable this project.

We will create a resource (in this case a house), and then issue tokens for that resource, and then transfer those tokens.


We create the representation of a house, within `CreateHouseTokenFlow.java`.


```java
public SignedTransaction call() throws FlowException {
    // Obtain a reference to a notary we wish to use.
    final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
    // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

    //create token type
    FungibleHouseTokenState evolvableTokenType = new FungibleHouseTokenState(valuation, getOurIdentity(),
                    new UniqueIdentifier(), 0, this.symbol);

    //wrap it with transaction state specifying the notary
    TransactionState<FungibleHouseTokenState> transactionState = new TransactionState<>(evolvableTokenType, notary);

    //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
    return subFlow(new CreateEvolvableTokens(transactionState));
}
```

We issue tokens `IssueHouseTokenFlow`

```java
public SignedTransaction call() throws FlowException {
    //get house states on ledger with uuid as input tokenId
    StateAndRef<FungibleHouseTokenState> stateAndRef = getServiceHub().getVaultService().
            queryBy(FungibleHouseTokenState.class).getStates().stream()
            .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
            .orElseThrow(()-> new IllegalArgumentException("FungibleHouseTokenState symbol=\""+symbol+"\" not found from vault"));

    //get the RealEstateEvolvableTokenType object
    FungibleHouseTokenState evolvableTokenType = stateAndRef.getState().getData();

    //create fungible token for the house token type
    FungibleToken fungibleToken = new FungibleTokenBuilder()
            .ofTokenType(evolvableTokenType.toPointer(FungibleHouseTokenState.class)) // get the token pointer
            .issuedBy(getOurIdentity())
            .heldBy(holder)
            .withAmount(quantity)
            .buildFungibleToken();

    //use built in flow for issuing tokens on ledger
    return subFlow(new IssueTokens(ImmutableList.of(fungibleToken)));
}
```

We then move the house token. `MoveHouseTokenFlow`

```java
public SignedTransaction call() throws FlowException {
    //get house states on ledger with uuid as input tokenId
    StateAndRef<FungibleHouseTokenState> stateAndRef = getServiceHub().getVaultService().
    queryBy(FungibleHouseTokenState.class).getStates().stream()
    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
    .orElseThrow(()-> new IllegalArgumentException("FungibleHouseTokenState symbol=\""+symbol+"\" not found from vault"));

    //get the RealEstateEvolvableTokenType object
    FungibleHouseTokenState tokenstate = stateAndRef.getState().getData();

    /*  specify how much amount to transfer to which holder
     *  Note: we use a pointer of tokenstate because it of type EvolvableTokenType
     */
    Amount<TokenType> amount = new Amount<>(quantity, tokenstate.toPointer(FungibleHouseTokenState.class));
    //PartyAndAmount partyAndAmount = new PartyAndAmount(holder, amount);

    //use built in flow to move fungible tokens to holder
    return subFlow(new MoveFungibleTokens(amount,holder));
}
```

## Pre-Requisites

For development environment setup, please refer to: [Setup Guide](https://docs.corda.net/getting-set-up.html).


## Usage

### Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```

### Interacting with the nodes

#### Shell

When started via the command line, each node will display an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.

    Tue July 09 11:58:13 GMT 2019>>>

You can use this shell to interact with your node.


Create house on the ledger using Seller's terminal

    flow start CreateHouseTokenFlow symbol: house, valuation: 100000

This will create a linear state of type HouseTokenState in Seller's vault

Seller will now issue some tokens to Buyer. run below command via Seller's terminal.

    flow start IssueHouseTokenFlow symbol: house, quantity: 50, holder: Buyer

Now at Buyer's terminal, we can check the tokens by running:
    flow start GetTokenBalance symbol: house

Since Buyer now has 50 tokens, Move tokens to Friend from Buyer s terminal

    flow start MoveHouseTokenFlow symbol: house, holder: Friend, quantity: 23

You can now view the number of Tokens held by both the Buyer and the friend by executing the following Query flow in their respective terminals.

    flow start GetTokenBalance symbol: house

