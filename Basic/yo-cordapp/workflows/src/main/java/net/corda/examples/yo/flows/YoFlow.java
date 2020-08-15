package net.corda.examples.yo.flows;

import co.paralleluniverse.fibers.Suspendable;
//import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.yo.contracts.YoContract;
import net.corda.examples.yo.states.YoState;
import org.jetbrains.annotations.Nullable;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;


import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
public class YoFlow extends FlowLogic<SignedTransaction> {
    private static final ProgressTracker.Step CREATING = new ProgressTracker.Step("Creating a new Yo!");
    private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing the Yo!");
    private static final ProgressTracker.Step VERIFYING = new ProgressTracker.Step("Verifying the Yo!");
    private static final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Sending the Yo!") {
        @Nullable
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    ProgressTracker progressTracker = new ProgressTracker(
            CREATING,
            SIGNING,
            VERIFYING,
            FINALISING
    );

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    private final String whoAmI;
    private final String whereTo;

    public YoFlow(String whoAmI, String whereTo) {
        this.whoAmI = whoAmI;
        this.whereTo = whereTo;
    }

    @Suspendable
    @Override
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

        // grab account service
        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        //grab the account information
        AccountInfo myAccount = accountService.accountInfo(whoAmI).get(0).getState().getData();
        AnonymousParty myAcconut = subFlow(new RequestKeyForAccount(myAccount));

        AccountInfo targetAccount = accountService.accountInfo(whereTo).get(0).getState().getData();
        AnonymousParty targetAcctAnonymousParty = subFlow(new RequestKeyForAccount(targetAccount));


        YoState state = new YoState(myAcconut, targetAcctAnonymousParty, "Yo to Accounts");
        TransactionBuilder utx = new TransactionBuilder(notary)
                .addOutputState(state)
                .addCommand(new YoContract.Commands.Send(),Arrays.asList(myAcconut.getOwningKey(),targetAcctAnonymousParty.getOwningKey()));

        progressTracker.setCurrentStep(VERIFYING);
        utx.verify(getServiceHub());

        progressTracker.setCurrentStep(SIGNING);
        SignedTransaction stx = getServiceHub().signInitialTransaction(utx,
                Arrays.asList(getOurIdentity().getOwningKey(),myAcconut.getOwningKey()));

        //Collect sigs
        FlowSession sessionForAccountToSendTo = initiateFlow(targetAccount.getHost());
        List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(new CollectSignatureFlow(stx,
                sessionForAccountToSendTo,targetAcctAnonymousParty.getOwningKey()));
        SignedTransaction signedByCounterParty = stx.withAdditionalSignatures(accountToMoveToSignature);

        progressTracker.setCurrentStep(FINALISING);
        subFlow(new FinalityFlow(signedByCounterParty,
                Arrays.asList(sessionForAccountToSendTo).stream().filter(it -> it.getCounterparty() != getOurIdentity()).collect(Collectors.toList())));
        subFlow(new SyncYo(whoAmI,targetAccount.getHost()));
        return null;
    }
}
