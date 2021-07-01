package net.corda.samples.election.accountUtilities;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.flows.StartableByService;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.samples.election.states.VoteState;

import java.util.*;

@StartableByRPC
@StartableByService
public class CountVotes extends FlowLogic<List<String>> {

//    private final String acctName;

//    public CountVotes(String acctname) {
//        this.acctName = acctname;
//    }
    public CountVotes() {

    }

    @Override
//    public List<StateAndRef<VoteState>> call() throws FlowException {
//    public List<StateAndRef<AccountInfo>> call() throws FlowException {
//    public List<String> call() throws FlowException {

    public List<String> call() throws FlowException {

//        AccountInfo myAccount = accountService.accountInfo(acctName).get(0).getState().getData();
//        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria()
//                .withExternalIds(Arrays.asList(myAccount.getIdentifier().getId()));
//        VaultCustomQueryCriteria criteria = new VaultCustomQueryCriteria(Vault.StateStatus.ALL);
//        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().getParticipants();
//        List<String> Votes = getServiceHub().getVaultService().queryBy(QueryCriteria.VaultQueryCriteria(externalIds = listOf(accountId))).getStates().stream().map(

        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
//        AccountInfo accountInfo = accountService.allAccounts().get(0).getState().getData();
        List<StateAndRef<AccountInfo>> accountInfo = accountService.allAccounts();
//        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria()
//                .withExternalIds(accountInfo.stream().map(
//                        it -> it.getState().getData().getIdentifier().getId()).collect(Collectors.toList()));
////                .withExternalIds(Collections.singletonList((accountInfo.getIdentifier().getId())));
//        List<String> Votes = null;
        for (StateAndRef<AccountInfo> account : accountInfo) {
            AccountInfo currentAccount = account.getState().getData();
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria()
                    .withExternalIds(Arrays.asList(currentAccount.getIdentifier().getId()));

//            System.out.println(getServiceHub().getVaultService().queryBy(VoteState.class,criteria).getStates().stream().map(
//                    it -> "\nVote State : " + it.getState().getData().getCandidate()).collect(Collectors.toList()));

            Vault.Page<LinearState> Votes = getServiceHub().getVaultService().queryBy(VoteState.class,criteria);

//            Votes.addAll(getServiceHub().getVaultService().queryBy(VoteState.class,criteria).getStates().stream().map(
//                    it -> "\nVote State : " + it.getState().getData().getCandidate()).collect(Collectors.toList()));
            System.out.println("\nCURRENTVOTES: " + Votes + "\nCURRENTCRITERIA: " + criteria + "\nCURRENTACCOUNT: " + currentAccount + "\nACCOUNT: " + account);
//            QueryCriteria participatingAccountCriteria = new QueryCriteria.VaultQueryCriteria()
//                    .withExternalIds(Collections.singletonList(
//                            account.getIdentifier().getId()));
//            QueryCriteria.VaultQueryCriteria crit = new QueryCriteria.VaultQueryCriteria().withExternalIds(Arrays.asList((account.getIdentifier().getId())));
//            System.out.println(account.g);
        }
//        val accounts: List<StateAndRef<AccountInfo>> = accountService.allAccounts();
//        val criteria: QueryCriteria.VaultQueryCriteria = QueryCriteria.VaultQueryCriteria()
//                .withExternalIds(accounts.map { it.state.data.identifier.id });
//        String candidate = accountInfo.get(0).getState().getData().getCandidate();
//
//        List<String> Votes = getServiceHub().getVaultService().queryBy(VoteState.class,criteria).getStates().stream().map(
//                it -> "\nVote State : " + it.getState().getData().getCandidate()).collect(Collectors.toList());
//
//        List<String> Participants = getServiceHub().getVaultService().queryBy(VoteState.class,criteria).getStates().stream().map(
//                it -> "\nParticipants: " + it.getState().getData().getParticipants()).collect(Collectors.toList());
////
//        Object Data = getServiceHub().getVaultService().queryBy(VoteState.class,criteria).getStates().stream().map(
//                it -> "\nData: " + it.getState().getData());

//        Object small = getServiceHub().getVaultService().queryBy(VoteState.class,criteria).getStates();
//        List<StateAndRef<VoteState>> voteData = getServiceHub().getVaultService().queryBy(VoteState.class,criteria).getStates();
//        String accountData = "\nVOTES: ";
//        for (StateAndRef<AccountInfo> account : accountInfo) {
//            accountData = accountData + "\n:  " + account.getState().getData().getIdentifier();
//        }
//        List<StateAndRef<VoteState>> VotesData = accountInfo.getServices().getVaultService().queryBy(VoteState.class,criteria).getStates();
//        List<String> VotesData = accountInfo.getServices().getVaultService().queryBy(VoteState.class,criteria).getStates();
//        VoteState VoteData = VotesData.get(0).getState().getData();

//        return Stream.of(InternalMessages, payments, Cargos,invoices,shippingRequest).flatMap(Collection::stream).collect(Collectors.toList());
//        return Stream.of(Votes, Participants, Data, VoteData).flatMap(Collection::stream).collect(Collectors.toList());
//        System.out.println("\nvotes: " + Votes);
//        System.out.println("criteria: " + criteria + "\nvotes: " + Votes + "\nData: " + Data + "\nsmall: " + small);
//        return Stream.of(Votes).flatMap(Collection::stream).collect(Collectors.toList());
//        return votesData;
//        return voteData;
//        return accountInfo;
        return null;
    }
}