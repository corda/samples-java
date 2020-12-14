package net.corda.samples.snl.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.commands.Create;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import com.sun.istack.NotNull;
import net.corda.core.contracts.ReferencedStateAndRef;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.sample.snl.states.BoardConfig;
import net.corda.sample.snl.states.GameBoard;

import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CreateGameFlow {

    private CreateGameFlow() {}

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<String>{

        private String player1;
        private String player2;

        public Initiator(String player1, String player2) {
            this.player1 = player1;
            this.player2 = player2;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);

            List<StateAndRef<AccountInfo>> p1accountInfo = accountService.accountInfo(player1);
            if(p1accountInfo.size() ==0)
                throw new FlowException("Player "+ player1 + " doesn't exist!");
            List<StateAndRef<AccountInfo>> p2accountInfo = accountService.accountInfo(player2);
            if(p1accountInfo.size() ==0)
                throw new FlowException("Player "+ player2 + " doesn't exist!");

            AbstractParty player1 = subFlow(new RequestKeyForAccount(p1accountInfo.get(0).getState().getData()));
            AbstractParty player2 = subFlow(new RequestKeyForAccount(p2accountInfo.get(0).getState().getData()));

            GameBoard gameBoard = new GameBoard(new UniqueIdentifier(null, UUID.randomUUID()),
                    player1, player2, this.player1, 1, 1, null, 0);

            List<StateAndRef<BoardConfig>> boardConfigList =
                    getServiceHub().getVaultService().queryBy(BoardConfig.class).getStates();
            if(boardConfigList.size() == 0)
                throw new FlowException("Please create board config first");

            TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                    .addOutputState(gameBoard)
                    .addCommand(new Create(), Arrays.asList(player1.getOwningKey(), player2.getOwningKey()))
                    .addReferenceState(new ReferencedStateAndRef<>(boardConfigList.get(0)));

            transactionBuilder.verify(getServiceHub());

            SignedTransaction selfSignedTransaction =
                    getServiceHub().signInitialTransaction(transactionBuilder, player1.getOwningKey());

            FlowSession player2Session =  initiateFlow(p2accountInfo.get(0).getState().getData().getHost());
            SignedTransaction signedTransaction =
                    subFlow(new CollectSignaturesFlow(selfSignedTransaction, Collections.singletonList(player2Session),
                            Collections.singletonList(player1.getOwningKey())));

            if(!p2accountInfo.get(0).getState().getData().getHost().equals(getOurIdentity())) {
                subFlow(new FinalityFlow(signedTransaction, Collections.singletonList(player2Session)));
                try {
                    accountService.shareStateAndSyncAccounts(signedTransaction
                            .toLedgerTransaction(getServiceHub(), false).outRef(0) ,player2Session.getCounterparty());
                } catch (SignatureException e) {
                    e.printStackTrace();
                }
            }else{
                subFlow(new FinalityFlow(signedTransaction, Collections.emptyList()));
            }
            return gameBoard.getLinearId().getId().toString();
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction>{
        private FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            subFlow(new SignTransactionFlow(counterpartySession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // Custom Logic to validate transaction.
                }
            });
            if(!counterpartySession.getCounterparty().equals(getOurIdentity()))
               return subFlow(new ReceiveFinalityFlow(counterpartySession));

            return null;
        }
    }
}
