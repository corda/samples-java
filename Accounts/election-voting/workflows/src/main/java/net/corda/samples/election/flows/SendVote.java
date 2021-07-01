package net.corda.samples.election.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import com.sun.istack.NotNull;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.election.contracts.VoteStateContract;
import net.corda.samples.election.accountUtilities.NewKeyForAccount;
import net.corda.samples.election.states.VoteState;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class SendVote extends FlowLogic<String> {

    //private variables
    private String whoAmI ;
    private final Party observer;
    private int candidate;

    //public constructor
    public SendVote(String whoAmI, Party observer, int candidate){
        this.whoAmI = whoAmI;
        this.observer = observer;
        this.candidate = candidate;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        //grab account service
        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        //grab the account information
        AccountInfo myAccount = accountService.accountInfo(whoAmI).get(0).getState().getData();
        PublicKey myKey = subFlow(new NewKeyForAccount(myAccount.getIdentifier().getId())).getOwningKey();

//        AccountInfo targetAccount = accountService.accountInfo(whereTo).get(0).getState().getData();
//        Party observerParty = subFlow(new RequestKeyForAccount(targetAccount));
        Party observerParty = observer;

        //generating State for transfer
        VoteState output = new VoteState(candidate,new AnonymousParty(myKey), observerParty);

        // Obtain a reference to a notary we wish to use.
        /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
         *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
         *
         *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
         */
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
        // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

        TransactionBuilder txbuilder = new TransactionBuilder(notary)
                .addOutputState(output)
                .addCommand(new VoteStateContract.Commands.Create(), Arrays.asList(observerParty.getOwningKey(),myKey));

        //self sign Transaction
        SignedTransaction locallySignedTx = getServiceHub().signInitialTransaction(txbuilder,Arrays.asList(getOurIdentity().getOwningKey(),myKey));

        //Collect sigs
//        FlowSession sessionForAccountToSendTo = initiateFlow(targetAccount.getHost());
        FlowSession sessionForAccountToSendTo = initiateFlow(observerParty);
        List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(new CollectSignatureFlow(locallySignedTx,
                sessionForAccountToSendTo, observerParty.getOwningKey()));
        SignedTransaction signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature);

        //Finalize
        subFlow(new FinalityFlow(signedByCounterParty,
                Arrays.asList(sessionForAccountToSendTo).stream().filter(it -> it.getCounterparty() != getOurIdentity()).collect(Collectors.toList())));
//        return "Payment send to " + targetAccount.getHost().getName().getOrganisation() + "'s "+ targetAccount.getName()+ " team.";
        return "Vote sent to " + observerParty.getName();
    }
}


@InitiatedBy(SendVote.class)
class SendVoteResponder extends FlowLogic<Void> {
    //private variable
    private FlowSession counterpartySession;

    //Constructor
    public SendVoteResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });
        subFlow(new ReceiveFinalityFlow(counterpartySession));
        return null;
    }
}

