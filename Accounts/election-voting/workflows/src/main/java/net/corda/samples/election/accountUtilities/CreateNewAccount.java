package net.corda.samples.election.accountUtilities;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;


@StartableByRPC
@StartableByService
@InitiatingFlow
public class CreateNewAccount extends FlowLogic<String>{

    private final String acctName;

    public CreateNewAccount(String acctName) {
        this.acctName = acctName;
    }

    @Override
    public String call() throws FlowException {
        StateAndRef<AccountInfo> newAccount = null;
       try {
           String acctHashString = subFlow(new HashAccount(acctName));
           newAccount = getServiceHub().cordaService(KeyManagementBackedAccountService.class).createAccount(acctHashString).get();
           System.out.println("Account created with hash: " + acctHashString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        AccountInfo acct = newAccount.getState().getData();
        return "" + acctName + "'s account was created. UUID is : " + acct.getIdentifier() + " and the Hash ID is: " + acct.getName();
    }
}
