package net.corda.examples.spaceships;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.money.DigitalCurrency;
import com.r3.corda.lib.tokens.money.MoneyUtilities;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import kotlin.Pair;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.identity.Party;
import net.corda.core.node.services.VaultService;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.spaceships.flows.*;
import net.corda.examples.spaceships.states.SpaceshipTokenType;
import net.corda.testing.common.internal.ParametersUtilitiesKt;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(
                new MockNetworkParameters()
                        .withCordappsForAllNodes(ImmutableList.of(
                                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
                                TestCordapp.findCordapp("net.corda.examples.spaceships.flows"),
                                TestCordapp.findCordapp("net.corda.examples.spaceships.contracts")
                        ))
                        .withNetworkParameters(ParametersUtilitiesKt.testNetworkParameters(
                                Collections.emptyList(), 4
                        ))
        );
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    /**
     * Test will issue two different currencies and check that they are committed to ledger of the holder
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void issuePlanetaryCurrency() throws ExecutionException, InterruptedException {
        Party partyA = a.getInfo().getLegalIdentities().get(0);

        IssuePlanetaryCurrencyFlows.Issue currencyFlow1 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "USD", 100);
        a.startFlow(currencyFlow1);

        IssuePlanetaryCurrencyFlows.Issue currencyFlow2 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "USD", 20);
        a.startFlow(currencyFlow2);

        IssuePlanetaryCurrencyFlows.Issue currencyFlow3 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "BTC", 30);
        a.startFlow(currencyFlow3);
        network.runNetwork();

        VaultService vs = a.getServices().getVaultService();

        // Use QueryUtilities to easily run various queries related to tokens
        // - Here we are grabbing the balance in PartyA's vault for each specified TokenType
        Amount<TokenType> fiatBalance = QueryUtilities.tokenBalance(vs, MoneyUtilities.getUSD());
        Amount<TokenType> digBalance = QueryUtilities.tokenBalance(vs, MoneyUtilities.getBTC());

        assert (12000L == fiatBalance.getQuantity()); // Quantities in tokenTypes are stored as long representations with the fractional digits
        assert (3000000000L == digBalance.getQuantity());
    }

    @Test
    public void tokenizeFungibleSpaceship() throws ExecutionException, InterruptedException {
        Party partyA = a.getInfo().getLegalIdentities().get(0);

        IssueSpaceshipFlows.TokenizeFungibleSpaceship delta5TokenizeFlow = new IssueSpaceshipFlows.TokenizeFungibleSpaceship(
                partyA,
                "Delta5",
                "Orion",
                150,
                "25 USD",
                25);
        CordaFuture<SignedTransaction> delta5FlowFuture = b.startFlow(delta5TokenizeFlow);

        IssueSpaceshipFlows.TokenizeFungibleSpaceship mach2TokenizeFlow = new IssueSpaceshipFlows.TokenizeFungibleSpaceship(
                partyA,
                "Mach2",
                "Earth",
                20,
                "50 GBP",
                300);
        CordaFuture<SignedTransaction> mach2FlowFuture = a.startFlow(mach2TokenizeFlow);

        network.runNetwork();
        SignedTransaction stx1 = delta5FlowFuture.get();
        SignedTransaction stx2 = mach2FlowFuture.get();

        FungibleToken delta5 = (FungibleToken) stx1.getCoreTransaction().getOutput(0);
        FungibleToken mach2 = (FungibleToken) stx2.getCoreTransaction().getOutput(0);

        /** AmountUtilities can generate Amount<IssuedTokenType> easily, among other functions.
         *  PartyA should now hold:
         *  25 tokens representing shares in a "Delta5" ship manufactured by PartyB
         *  300 tokens representing shares in a "Mach2" ship manufactured by itself (PartyA)
         */
        Amount<IssuedTokenType> delta5ExpectedAmount = AmountUtilities.amount(25, delta5.getIssuedTokenType());
        Amount<IssuedTokenType> mach2ExpectedAmount = AmountUtilities.amount(300, mach2.getIssuedTokenType());
        assert (delta5.getAmount().equals(delta5ExpectedAmount));
        assert (mach2.getAmount().equals(mach2ExpectedAmount));


        VaultService vs = a.getServices().getVaultService();
        // Use QueryUtilities to easily run various queries related to tokens
        // - Here we are returning a list of all FungibleTokens which match the TokenType argument in PartyA's vault.
        // - tokenAmountsByToken is for querying FungibleTokens
        List<StateAndRef<FungibleToken>> delta5OnLedger = QueryUtilities.tokenAmountsByToken(vs, delta5.getTokenType()).getStates();
        List<StateAndRef<FungibleToken>> mach2OnLedger = QueryUtilities.tokenAmountsByToken(vs, mach2.getTokenType()).getStates();

        // We should have exactly 1 token of each type issued to PartyA
        assert (delta5OnLedger.size() == 1);
        assert (mach2OnLedger.size() == 1);
    }

    @Test
    public void tokenizeNonFungibleSpaceship() throws ExecutionException, InterruptedException {
        Party partyA = a.getInfo().getLegalIdentities().get(0);

        IssueSpaceshipFlows.TokenizeNonFungibleSpaceship uniqueGammaTokenizeFlow = new IssueSpaceshipFlows.TokenizeNonFungibleSpaceship(
                partyA,
                "UniqueGamma",
                "Saturn",
                10,
                "1 BTC");
        CordaFuture<SignedTransaction> uniqueGammaFlowFuture = a.startFlow(uniqueGammaTokenizeFlow);
        network.runNetwork();

        SignedTransaction stx = uniqueGammaFlowFuture.get();

        NonFungibleToken uniqueGamma = (NonFungibleToken) stx.getCoreTransaction().getOutput(0);

        VaultService vs = a.getServices().getVaultService();
        // Use QueryUtilities to easily run various queries related to tokens
        // - Here we are returning a list of all NonFungibleTokens which match the TokenType argument in PartyA's vault.
        // - heldTokensByToken is for querying NonFungibleTokens
        List<StateAndRef<NonFungibleToken>> uniqueGammaTokens = QueryUtilities.heldTokensByToken(vs, uniqueGamma.getTokenType()).getStates();

        assert (uniqueGammaTokens.size() == 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateSpaceship() throws ExecutionException, InterruptedException {
        Party partyA = a.getInfo().getLegalIdentities().get(0);

        // Issue an original ship

        IssueSpaceshipFlows.TokenizeNonFungibleSpaceship uniqueGammaTokenizeFlow = new IssueSpaceshipFlows.TokenizeNonFungibleSpaceship(
                partyA,
                "UniqueGamma",
                "Saturn",
                10,
                "1 BTC");
        CordaFuture<SignedTransaction> uniqueGammaFlowFuture = a.startFlow(uniqueGammaTokenizeFlow);
        network.runNetwork();

        SignedTransaction stx = uniqueGammaFlowFuture.get();

        NonFungibleToken uniqueGammaToken = (NonFungibleToken) stx.getCoreTransaction().getOutput(0);

        @SuppressWarnings("unchecked")
        StateAndRef<SpaceshipTokenType> originalShipStateAndRef = ((TokenPointer<SpaceshipTokenType>) uniqueGammaToken.getTokenType()).getPointer().resolve(a.getServices());

        // Update the ship

        Amount<TokenType> updatedValue = MoneyUtilities.USD(150);
        UpdateSpaceshipFlow updateShipFlow = new UpdateSpaceshipFlow(originalShipStateAndRef, 3, updatedValue);

        CordaFuture<SignedTransaction> updateShipFuture = a.startFlow(updateShipFlow);
        network.runNetwork();

        SignedTransaction stx2 = updateShipFuture.get();

        SpaceshipTokenType originalShip = originalShipStateAndRef.getState().getData();
        SpaceshipTokenType updatedShip = (SpaceshipTokenType) stx2.getCoreTransaction().getOutput(0);
        System.out.println("Original ship details : " +  "seating: " + originalShip.getSeatingCapacity() +
                " value : " + originalShip.getValue().toDecimal() + " " + originalShip.getValue().getToken().getTokenIdentifier() + "\n");
        System.out.println("Updated ship details : " +  "seating: " + updatedShip.getSeatingCapacity() +
                " value : " + updatedShip.getValue().toDecimal() + " " + updatedShip.getValue().getToken().getTokenIdentifier() + "\n");

        // Make sure that PartyA's token will resolve to the updated definition

        // Use QueryUtilities to easily run various queries related to tokens
        // - Here we are returning a list of all NonFungibleTokens which match the TokenType argument in PartyA's vault.
        // - heldTokensByToken is for querying NonFungibleTokens
        TokenPointer<SpaceshipTokenType> currentSpaceShipToken = (TokenPointer<SpaceshipTokenType>) QueryUtilities.heldTokensByToken(a.getServices().getVaultService(), uniqueGammaToken.getTokenType())
                .getStates().get(0).getState().getData().getTokenType();
        SpaceshipTokenType shipDefInPartyAVault = currentSpaceShipToken.getPointer().resolve(a.getServices()).getState().getData();

        assert updatedShip.getLinearId().equals(shipDefInPartyAVault.getLinearId());
        assert updatedShip.getSeatingCapacity() == shipDefInPartyAVault.getSeatingCapacity();
        assert updatedShip.getValue().equals(shipDefInPartyAVault.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    // PartyA is holder/seller, PartyB is buyer (flow is initiated by the buyer)
    public void buySpaceShipFromHolder() throws ExecutionException, InterruptedException {
        Party partyA = a.getInfo().getLegalIdentities().get(0);
        Party partyB = b.getInfo().getLegalIdentities().get(0);

        // Issue an original ship to PartyA

        IssueSpaceshipFlows.TokenizeNonFungibleSpaceship uniqueGammaTokenizeFlow = new IssueSpaceshipFlows.TokenizeNonFungibleSpaceship(
                partyA,
                "UniqueGamma",
                "Saturn",
                10,
                "1500 USD");
        CordaFuture<SignedTransaction> uniqueGammaFlowFuture = a.startFlow(uniqueGammaTokenizeFlow);
        network.runNetwork();

        SignedTransaction stx = uniqueGammaFlowFuture.get();

        NonFungibleToken uniqueGammaToken = (NonFungibleToken) stx.getCoreTransaction().getOutput(0);

        SpaceshipTokenType originalShipState = ((TokenPointer<SpaceshipTokenType>) uniqueGammaToken.getTokenType()).getPointer().resolve(a.getServices()).getState().getData();
        String shipId = originalShipState.getLinearId().getId().toString(); // Save the shipId so that PartyB can propose to buy

        // Issue some planetary Currency to PartyB
        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyBFlow = new IssuePlanetaryCurrencyFlows.Issue(partyB, "USD", 5000);
        b.startFlow(issueCurrencyPartyBFlow);
        network.runNetwork();

        // Value of the ship is 1500 USD, and we will let PartyB buy the ship using their 5000 USD
        BuySpaceshipFlows.BuySpaceshipInitiator buyShipFlow = new BuySpaceshipFlows.BuySpaceshipInitiator(shipId, partyA);
        CordaFuture<SignedTransaction> buyShipFlowFuture = b.startFlow(buyShipFlow);
        network.runNetwork();

        SignedTransaction shipBoughtStx = buyShipFlowFuture.get();

        // check vaults of PartyA and PartyB that they have the correct contents
        // PartyA - 1500 USD received from the Sale
        // PartyB - 1 NonFungibleTokenRepresenting ownership of the ship, 3500 USD change from the Sale
        VaultService vsA = a.getServices().getVaultService();
        VaultService vsB = b.getServices().getVaultService();

        Amount<TokenType> partyAHeldUSD = QueryUtilities.tokenBalance(vsA, MoneyUtilities.getUSD());
        List<StateAndRef<NonFungibleToken>> partyAHeldShips = QueryUtilities.heldTokensByToken(vsA, uniqueGammaToken.getTokenType()).getStates();
        Amount<TokenType> partyBHeldUSD = QueryUtilities.tokenBalance(vsB, MoneyUtilities.getUSD());
        List<StateAndRef<NonFungibleToken>> partyBHeldShips = QueryUtilities.heldTokensByToken(vsB, uniqueGammaToken.getTokenType()).getStates();

        assert (partyAHeldUSD.equals(MoneyUtilities.USD(1500)));
        assert (partyAHeldShips.isEmpty());
        assert (partyBHeldUSD.equals(MoneyUtilities.USD(3500)));

        // resolve the ship partyB now holds
        assert (partyBHeldShips.size() == 1);
        SpaceshipTokenType shipHeldByPartyB = ((TokenPointer<SpaceshipTokenType>) partyBHeldShips.get(0).getState().getData().getTokenType()).getPointer().resolve(b.getServices()).getState().getData();
        assert originalShipState.getLinearId().equals(shipHeldByPartyB.getLinearId());
        assert originalShipState.getSeatingCapacity() == shipHeldByPartyB.getSeatingCapacity();
        assert originalShipState.getValue().equals(shipHeldByPartyB.getValue());

    }

    @Test
    public void querySharesOwnedInSpaceship() throws ExecutionException, InterruptedException, TransactionResolutionException {
        Party partyA = a.getInfo().getLegalIdentities().get(0);

        IssueSpaceshipFlows.TokenizeFungibleSpaceship delta5TokenizeFlow = new IssueSpaceshipFlows.TokenizeFungibleSpaceship(
                partyA,
                "Delta5",
                "Orion",
                150,
                "25 USD",
                25);
        CordaFuture<SignedTransaction> delta5FlowFuture = a.startFlow(delta5TokenizeFlow);
        network.runNetwork();
        SignedTransaction stx1 = delta5FlowFuture.get();
        FungibleToken delta5Token = stx1.getCoreTransaction().outputsOfType(FungibleToken.class).get(0);

        SpaceshipTokenType delta5Ship = (SpaceshipTokenType) a.getServices().toStateAndRef(stx1.getReferences().get(0)).getState().getData();

        String shipId = delta5Ship.getLinearId().getId().toString(); // Save the shipId so that PartyB can propose to buy

        InvestSpaceshipFlows.sharesOwnedInSpaceshipInitiator querySharesFlow = new InvestSpaceshipFlows.sharesOwnedInSpaceshipInitiator(shipId, partyA);
        CordaFuture<Pair<BigDecimal, Amount<TokenType>>> querySharesFuture = b.startFlow(querySharesFlow);
        network.runNetwork();

        Pair<BigDecimal, Amount<TokenType>> querySharesResult = querySharesFuture.get();

        System.out.println(querySharesResult.getFirst());
        System.out.println(querySharesResult.getSecond().toDecimal());
        assert (delta5Token.getAmount().toDecimal().equals(querySharesResult.getFirst()));
        assert (querySharesResult.getSecond().toDecimal().doubleValue() == 25.00);
    }

    @Test
    public void purchaseSharesInSpaceship() throws ExecutionException, InterruptedException, TransactionResolutionException {
        Party partyA = a.getInfo().getLegalIdentities().get(0);
        Party partyB = b.getInfo().getLegalIdentities().get(0);

        // Issue some planetary Currency to PartyB
        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyBFlow = new IssuePlanetaryCurrencyFlows.Issue(partyB, "USD", 50000);
        b.startFlow(issueCurrencyPartyBFlow);
        network.runNetwork();

        // Issue fungible spaceship shares to partyA
        IssueSpaceshipFlows.TokenizeFungibleSpaceship delta5TokenizeFlow = new IssueSpaceshipFlows.TokenizeFungibleSpaceship(
                partyA,
                "Delta5",
                "Orion",
                150,
                "25 USD",
                1000);
        CordaFuture<SignedTransaction> delta5FlowFuture = a.startFlow(delta5TokenizeFlow);
        network.runNetwork();
        SignedTransaction stx1 = delta5FlowFuture.get();
        FungibleToken delta5Token = stx1.getCoreTransaction().outputsOfType(FungibleToken.class).get(0);

        SpaceshipTokenType delta5Ship = (SpaceshipTokenType) a.getServices().toStateAndRef(stx1.getReferences().get(0)).getState().getData();

        String shipId = delta5Ship.getLinearId().getId().toString(); // Save the shipId so that PartyB can propose to buy

        InvestSpaceshipFlows.sharesOwnedInSpaceshipInitiator querySharesFlow = new InvestSpaceshipFlows.sharesOwnedInSpaceshipInitiator(shipId, partyA);
        CordaFuture<Pair<BigDecimal, Amount<TokenType>>> querySharesFuture = b.startFlow(querySharesFlow);
        network.runNetwork();

        Pair<BigDecimal, Amount<TokenType>> querySharesResult = querySharesFuture.get();

        // Buy portion of shares (20% of the sellers holdings) (partyB - buyer, partyA - seller)
        BigDecimal numShares = querySharesResult.getFirst().multiply(BigDecimal.valueOf(0.2)).setScale(4, BigDecimal.ROUND_DOWN);
        Amount<TokenType> paymentAmount = querySharesResult.getSecond().times(numShares.longValue()); // share value * numShares
        InvestSpaceshipFlows.BuySharesInSpaceshipInitiator buyingFlow = new InvestSpaceshipFlows.BuySharesInSpaceshipInitiator(numShares, paymentAmount, shipId, partyA);
        CordaFuture<SignedTransaction> buyingFlowFuture = b.startFlow(buyingFlow);
        network.runNetwork();


        // check vaults of PartyA and PartyB that they have the correct contents
        // PartyA - 5000 USD received from the Sale, and 800 units left of shares in the ship
        // PartyB - 45000 USD remainder from the Sale, and 200 units of shares in the ship
        VaultService vsA = a.getServices().getVaultService();
        VaultService vsB = b.getServices().getVaultService();

        Amount<TokenType> partyAHeldUSD = QueryUtilities.tokenBalance(vsA, MoneyUtilities.getUSD());
        Amount<TokenType> partyAHeldShares = QueryUtilities.tokenBalance(vsA, delta5Token.getTokenType());
        Amount<TokenType> partyBHeldUSD = QueryUtilities.tokenBalance(vsB, MoneyUtilities.getUSD());
        Amount<TokenType> partyBHeldShares = QueryUtilities.tokenBalance(vsB, delta5Token.getTokenType());

        assert partyAHeldUSD.equals(MoneyUtilities.USD(5000));
        assert partyAHeldShares.equals(AmountUtilities.amount(800, delta5Token.getTokenType()));
        assert partyBHeldUSD.equals(MoneyUtilities.USD(45000));
        assert partyBHeldShares.equals(AmountUtilities.amount(200, delta5Token.getTokenType()));

        // Buy ALL REMAINING shares available
        InvestSpaceshipFlows.sharesOwnedInSpaceshipInitiator querySharesFlow2 = new InvestSpaceshipFlows.sharesOwnedInSpaceshipInitiator(shipId, partyA);
        CordaFuture<Pair<BigDecimal, Amount<TokenType>>> querySharesFuture2 = b.startFlow(querySharesFlow2);
        network.runNetwork();

        Pair<BigDecimal, Amount<TokenType>> querySharesResult2 = querySharesFuture2.get();

        // Buy shares (partyB - buyer, partyA - seller)
        numShares = querySharesResult2.getFirst();
        paymentAmount = querySharesResult2.getSecond().times(numShares.longValue()); // share value * numShares
        InvestSpaceshipFlows.BuySharesInSpaceshipInitiator buyingFlow2 = new InvestSpaceshipFlows.BuySharesInSpaceshipInitiator(numShares, paymentAmount, shipId, partyA);
        CordaFuture<SignedTransaction> buyingFlowFuture2 = b.startFlow(buyingFlow2);
        network.runNetwork();

        // check vaults of PartyA and PartyB that they have the correct contents
        // PartyA - 25000 USD (5000 from previous sale, 20000 from this sale), and 0 units left of shares in the ship
        // PartyB - 25000 USD (remainder from previous sale and this sale), and 1000 units of shares in the ship
        partyAHeldUSD = QueryUtilities.tokenBalance(vsA, MoneyUtilities.getUSD());
        partyAHeldShares = QueryUtilities.tokenBalance(vsA, delta5Token.getTokenType());
        partyBHeldUSD = QueryUtilities.tokenBalance(vsB, MoneyUtilities.getUSD());
        partyBHeldShares = QueryUtilities.tokenBalance(vsB, delta5Token.getTokenType());

        assert partyAHeldUSD.equals(MoneyUtilities.USD(25000));
        assert partyAHeldShares.equals(AmountUtilities.amount(0, delta5Token.getTokenType()));
        assert partyBHeldUSD.equals(MoneyUtilities.USD(25000));
        assert partyBHeldShares.equals(AmountUtilities.amount(1000, delta5Token.getTokenType()));
    }

    @Test
    public void utilityTestLooseChangeFinder() throws ExecutionException, InterruptedException {
        Party partyA = a.getInfo().getLegalIdentities().get(0);

        // issue various currencies to partyA
        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyAFlow1 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "USD", 500.59);
        a.startFlow(issueCurrencyPartyAFlow1);

        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyAFlow2 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "AUD", 510000);
        a.startFlow(issueCurrencyPartyAFlow2);

        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyAFlow3 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "BTC", 2);
        a.startFlow(issueCurrencyPartyAFlow3);

        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyAFlow4 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "GBP", 520.80);
        a.startFlow(issueCurrencyPartyAFlow4);

        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyAFlow5 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "USD", 600.00);
        a.startFlow(issueCurrencyPartyAFlow5);

        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyAFlow6 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "USD", 130.29);
        a.startFlow(issueCurrencyPartyAFlow6);

        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyAFlow7 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "USD", 13123.00);
        a.startFlow(issueCurrencyPartyAFlow7);

        network.runNetwork();

        // TEST LooseChangeFinder

        UtilityFlows.LooseChangeFinderFlow looseChangeFinderFlow = new UtilityFlows.LooseChangeFinderFlow(MoneyUtilities.getUSD());
        CordaFuture<List<StateAndRef<FungibleToken>>> looseChangeFuture = a.startFlow(looseChangeFinderFlow);
        network.runNetwork();
        List<StateAndRef<FungibleToken>> looseChangeTokens = looseChangeFuture.get();

        // Check if the looseChangeTokens ONLY contains the instances of fractional amounts
        assert looseChangeTokens.size() == 2;
        List<Amount<TokenType>> amountsInRetrievedTokens = looseChangeTokens.stream().map(it ->
                new Amount<>(it.getState().getData().getAmount().getQuantity(), it.getState().getData().getAmount().getToken().getTokenType()))
                .collect(Collectors.toList());
        assert amountsInRetrievedTokens.contains(MoneyUtilities.USD(130.29));
        assert amountsInRetrievedTokens.contains(MoneyUtilities.USD(500.59));

    }

    @Test
    public void utilityTestNetWorth() throws ExecutionException, InterruptedException {
        Party partyA = a.getInfo().getLegalIdentities().get(0);

        // issue various currencies to partyA

        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyAFlow2 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "AUD", 510000);
        a.startFlow(issueCurrencyPartyAFlow2);

        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyAFlow3 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "BTC", 2);
        a.startFlow(issueCurrencyPartyAFlow3);

        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyAFlow5 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "USD", 600.00);
        a.startFlow(issueCurrencyPartyAFlow5);

        IssuePlanetaryCurrencyFlows.Issue issueCurrencyPartyAFlow6 = new IssuePlanetaryCurrencyFlows.Issue(partyA, "USD", 100.00);
        a.startFlow(issueCurrencyPartyAFlow6);

        network.runNetwork();

        // TEST GetTotalNetWorth is USD

        UtilityFlows.GetTotalNetWorthFlow netWorthFlowUSD = new UtilityFlows.GetTotalNetWorthFlow(MoneyUtilities.getUSD());
        CordaFuture<Amount<TokenType>> netWorthFutureUSD = a.startFlow(netWorthFlowUSD);
        network.runNetwork();
        Amount<TokenType> netWorthResultUSD = netWorthFutureUSD.get();

        assert netWorthResultUSD.toDecimal().doubleValue() == 308700.00;

        // TEST GetTotalNetWorth in BTC

        UtilityFlows.GetTotalNetWorthFlow netWorthFlowBTC = new UtilityFlows.GetTotalNetWorthFlow(DigitalCurrency.getInstance("BTC"));
        CordaFuture<Amount<TokenType>> netWorthFutureBTC = a.startFlow(netWorthFlowBTC);
        network.runNetwork();
        Amount<TokenType> netWorthResultBTC = netWorthFutureBTC.get();

        // BTC has a fractionalDigit of 8 so amount is 308.7 bitcoin
        assert netWorthResultBTC.getQuantity() == 30870000000L;

        // TEST GetTotalNetWorth in AUD

        UtilityFlows.GetTotalNetWorthFlow netWorthFlowAUD = new UtilityFlows.GetTotalNetWorthFlow(MoneyUtilities.getAUD());
        CordaFuture<Amount<TokenType>> netWorthFutureAUD = a.startFlow(netWorthFlowAUD);
        network.runNetwork();
        Amount<TokenType> netWorthResultAUD = netWorthFutureAUD.get();

        assert netWorthResultAUD.toDecimal().doubleValue() == 514499.99;

        // TEST GetTotalNetWorth in AUD W/ Exlusion of BTC

        UtilityFlows.GetTotalNetWorthFlow netWorthFlowAUDEx = new UtilityFlows.GetTotalNetWorthFlow(MoneyUtilities.getAUD(), Collections.singletonList(DigitalCurrency.getInstance("BTC")));
        CordaFuture<Amount<TokenType>> netWorthFutureAUDEx = a.startFlow(netWorthFlowAUDEx);
        network.runNetwork();
        Amount<TokenType> netWorthResultAUDEx = netWorthFutureAUDEx.get();

        assert netWorthResultAUDEx.toDecimal().doubleValue() == 511166.66;
    }


}
