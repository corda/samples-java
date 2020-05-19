package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlow;
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlowHandler;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class TokenIssuanceFlow extends FlowLogic<SignedTransaction> {

    private final String issuer;
    private final String owner;
    private final int amount;

    public TokenIssuanceFlow(String issuer, String owner, int amount) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        AccountInfo issuerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(issuer).get(0).getState().getData();
        AccountInfo ownerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(owner).get(0).getState().getData();

        AnonymousParty issuerAccount = subFlow(new RequestKeyForAccount(issuerAccountInfo));//self node
        AnonymousParty ownerAccount = subFlow(new RequestKeyForAccount(ownerAccountInfo));

        FlowSession ownerSession = initiateFlow(ownerAccountInfo.getHost());

        subFlow(new SyncKeyMappingFlow(ownerSession, Arrays.asList(issuerAccount)));

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //create a transactionBuilder
        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
        TokenState tokenState = new TokenState(issuerAccount, ownerAccount , amount);

        transactionBuilder.addOutputState(tokenState);
        transactionBuilder.addCommand(new TokenContract.Commands.Issue() ,
                ImmutableList.of(issuerAccount.getOwningKey(),ownerAccount.getOwningKey()));

        List<CommandWithParties<CommandData>> commandWithPartiesList  = transactionBuilder.toLedgerTransaction(getServiceHub()).getCommands();

        List<PublicKey> mySigners = new ArrayList();

        for(CommandWithParties<CommandData> commandDataCommandWithParties : commandWithPartiesList) {
            if(((ArrayList<PublicKey>)(getServiceHub().getKeyManagementService().filterMyKeys(commandDataCommandWithParties.getSigners()))).size() > 0) {
                mySigners.add(((ArrayList<PublicKey>)getServiceHub().getKeyManagementService().filterMyKeys(commandDataCommandWithParties.getSigners())).get(0));
            }
        }

        //sign the transaction with the signers we got by calling filterMyKeys
        SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder, mySigners);

        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                selfSignedTransaction,
                Collections.singletonList(ownerSession),
                mySigners));

        //call FinalityFlow for finality
        subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(ownerSession)));

        return null;

    }
}

@InitiatedBy(TokenIssuanceFlow.class)
class TokenIssuanceFlowResponder extends FlowLogic<Void> {

    private final FlowSession otherSide;

    public TokenIssuanceFlowResponder(FlowSession otherSide) {
        this.otherSide = otherSide;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {

        subFlow(new SyncKeyMappingFlowHandler(otherSide));

        subFlow(new SignTransactionFlow(otherSide) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });
        subFlow(new ReceiveFinalityFlow(otherSide));

        return null;
    }
}