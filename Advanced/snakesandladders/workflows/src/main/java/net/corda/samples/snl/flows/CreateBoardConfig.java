package net.corda.samples.snl.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.commands.Create;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import com.sun.istack.NotNull;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.sample.snl.states.BoardConfig;

import java.security.SignatureException;
import java.util.*;

public class CreateBoardConfig {
    private CreateBoardConfig() {}

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private String player1;
        private String player2;

        public Initiator(String player1, String player2) {
            this.player1 = player1;
            this.player2 = player2;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
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

            Map<Integer, Integer> ladderPositions = new LinkedHashMap<>();
            ladderPositions.put(2, 45);
            ladderPositions.put(4, 27);
            ladderPositions.put(9, 31);
            ladderPositions.put(47, 84);
            ladderPositions.put(70, 87);
            ladderPositions.put(71, 91);

            Map<Integer, Integer> snakePositions = new LinkedHashMap<>();
            snakePositions.put(16, 8);
            snakePositions.put(52, 28);
            snakePositions.put(78, 25);
            snakePositions.put(93, 89);
            snakePositions.put(95, 75);
            snakePositions.put(99, 21);

            BoardConfig boardConfig = new BoardConfig(ladderPositions, snakePositions, Arrays.asList(player1, player2));

            TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                    .addOutputState(boardConfig)
                    .addCommand(new Create(), Arrays.asList(player1.getOwningKey(), player2.getOwningKey()));

            transactionBuilder.verify(getServiceHub());

            SignedTransaction selfSignedTransaction =
                    getServiceHub().signInitialTransaction(transactionBuilder, player1.getOwningKey());

            FlowSession player2Session =  initiateFlow(p2accountInfo.get(0).getState().getData().getHost());
            SignedTransaction signedTransaction =
                    subFlow(new CollectSignaturesFlow(selfSignedTransaction, Collections.singletonList(player2Session),
                            Collections.singleton(player1.getOwningKey())));

            if(!p2accountInfo.get(0).getState().getData().getHost().equals(getOurIdentity())) {
                signedTransaction =  subFlow(new FinalityFlow(signedTransaction, Collections.singletonList(player2Session)));
                try {
                    accountService.shareStateAndSyncAccounts(signedTransaction
                            .toLedgerTransaction(getServiceHub()).outRef(0) ,player2Session.getCounterparty());
                } catch (SignatureException e) {
                    e.printStackTrace();
                }
                return signedTransaction;
            }else{
                return subFlow(new FinalityFlow(signedTransaction, Collections.emptyList()));
            }
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
                return  subFlow(new ReceiveFinalityFlow(counterpartySession));

            return null;
        }
    }
}
