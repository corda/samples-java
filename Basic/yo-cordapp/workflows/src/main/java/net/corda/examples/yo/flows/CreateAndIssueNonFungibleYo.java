package net.corda.examples.yo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import com.r3.corda.lib.tokens.workflows.utilities.NonFungibleTokenBuilder;
import net.corda.core.flows.*;
import net.corda.core.node.services.IdentityService;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.yo.states.YoTokenFungible;
import net.corda.examples.yo.states.YoTokenNonFungible;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Designed initiating node : Company
 * This flow issues a stock to the node itself just to keep things simple
 * ie. the company and the recipient of IssueTokens are the same
 * It first creates a StockState as EvovableTokenType and then issues some tokens base on this EvovableTokenType
 * The observer receives a copy of all of the transactions and records it in their vault
 */
@InitiatingFlow
@StartableByRPC
public class CreateAndIssueNonFungibleYo extends FlowLogic<SignedTransaction> {

    private static final ProgressTracker.Step CREATING = new ProgressTracker.Step("Creating the Non-fungible Yo Token!");
    private static final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Sending the Non-fungible Yo Token!") {
        @Nullable
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    ProgressTracker progressTracker = new ProgressTracker(
            CREATING,
            FINALISING
    );

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    private final Party target;

    public CreateAndIssueNonFungibleYo(Party target) {
        this.target = target;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        progressTracker.setCurrentStep(CREATING);
        // Obtain a reference to a notary we wish to use.
        /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
         *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
         *
         *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
         */
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
        // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

        YoTokenNonFungible yo = new YoTokenNonFungible(getOurIdentity());

        // The notary provided here will be used in all future actions of this token
        TransactionState<YoTokenNonFungible> transactionState = new TransactionState<>(yo, notary);

        // Using the build-in flow to create an evolvable token type -- Stock
        subFlow(new CreateEvolvableTokens(transactionState));

        // Indicate the recipient which is the issuing party itself here
        //new FungibleToken(issueAmount, getOurIdentity(), null);
        NonFungibleToken yoFungibleToken = new NonFungibleTokenBuilder()
                .ofTokenType(yo.toPointer())
                .issuedBy(getOurIdentity())
                .heldBy(target)
                .buildNonFungibleToken();

        progressTracker.setCurrentStep(FINALISING);
        SignedTransaction stx = subFlow(new IssueTokens(Arrays.asList(yoFungibleToken)));
        return stx;
    }
}
