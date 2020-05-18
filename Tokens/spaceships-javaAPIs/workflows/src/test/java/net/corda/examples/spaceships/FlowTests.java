package net.corda.examples.spaceships;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.money.DigitalCurrency;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import net.corda.core.node.services.VaultService;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.spaceships.flows.IssuePlanetaryCurrencyFlows;
import net.corda.examples.spaceships.flows.IssueSpaceShipFlows;
import net.corda.testing.common.internal.ParametersUtilitiesKt;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                                TestCordapp.findCordapp("com.r3.corda.lib.tokens.money"),
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

        IssuePlanetaryCurrencyFlows.Initiator flow = new IssuePlanetaryCurrencyFlows.Initiator(partyA, "USD", 100);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);

        IssuePlanetaryCurrencyFlows.Initiator flow2 = new IssuePlanetaryCurrencyFlows.Initiator(partyA, "USD", 20);
        CordaFuture<SignedTransaction> future2 = a.startFlow(flow2);

        IssuePlanetaryCurrencyFlows.Initiator flow3 = new IssuePlanetaryCurrencyFlows.Initiator(partyA, "BTC", 30);
        CordaFuture<SignedTransaction> future3 = a.startFlow(flow3);
        network.runNetwork();

        VaultService vs = a.getServices().getVaultService();

        // Use QueryUtilities to easily run various queries related to tokens
        // - Here we are grabbing the balance in PartyA's vault for each specified TokenType
        Amount<TokenType> fiatBalance = QueryUtilities.tokenBalance(vs, FiatCurrency.getInstance("USD"));
        Amount<TokenType> digBalance = QueryUtilities.tokenBalance(vs, DigitalCurrency.getInstance("BTC"));

        assert (12000L == fiatBalance.getQuantity()); // Quantities in tokenTypes are stored as long representations with the fractional digits
        assert (3000000000L == digBalance.getQuantity());
    }

    @Test
    public void tokenizeFungibleSpaceship() throws ExecutionException, InterruptedException {
        Party partyA = a.getInfo().getLegalIdentities().get(0);

        IssueSpaceShipFlows.TokenizeFungibleSpaceship flow = new IssueSpaceShipFlows.TokenizeFungibleSpaceship(
                partyA,
                "Delta5",
                "Orion",
                150,
                "25 USD",
                25);
        CordaFuture<SignedTransaction> future = b.startFlow(flow);

        IssueSpaceShipFlows.TokenizeFungibleSpaceship flow1 = new IssueSpaceShipFlows.TokenizeFungibleSpaceship(
                partyA,
                "Mach2",
                "Earth",
                20,
                "50 GBP",
                300);
        CordaFuture<SignedTransaction> future1 = a.startFlow(flow1);

        network.runNetwork();
        SignedTransaction stx1 = future.get();
        SignedTransaction stx2 = future1.get();

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

        IssueSpaceShipFlows.TokenizeNonFungibleSpaceship flow = new IssueSpaceShipFlows.TokenizeNonFungibleSpaceship(
                partyA,
                "UniqueGamma",
                "Saturn",
                10,
                "1 BTC");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction stx = future.get();

        NonFungibleToken uniqueGamma = (NonFungibleToken) stx.getCoreTransaction().getOutput(0);

        VaultService vs = a.getServices().getVaultService();
        // Use QueryUtilities to easily run various queries related to tokens
        // - Here we are returning a list of all NonFungibleTokens which match the TokenType argument in PartyA's vault.
        // - heldTokensByToken is for querying NonFungibleTokens
        List<StateAndRef<NonFungibleToken>> uniqueGammaTokens = QueryUtilities.heldTokensByToken(vs, uniqueGamma.getTokenType()).getStates();

        assert (uniqueGammaTokens.size() == 1);
    }

}
