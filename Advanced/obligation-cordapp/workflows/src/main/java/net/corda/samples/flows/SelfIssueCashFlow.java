package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.CashIssueFlow;
import java.util.Currency;

@InitiatingFlow
@StartableByRPC
public class SelfIssueCashFlow extends FlowLogic<Cash.State> {

    private Amount<Currency> amount;

    public SelfIssueCashFlow(Amount<Currency> amount) {
        this.amount = amount;
    }

    @Suspendable
    public Cash.State call() throws FlowException {
        /** Create the cash issue command. */
        OpaqueBytes issueRef = OpaqueBytes.of("1".getBytes());

        // Obtain a reference to a notary we wish to use.
        /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
         *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
         *
         *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
         */
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
        // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

        /** Create the cash issuance transaction. */
        SignedTransaction cashIssueTransaction = subFlow(new CashIssueFlow(amount, issueRef, notary)).getStx();
        /** Return the cash output. */
        return (Cash.State) cashIssueTransaction.getTx().getOutputs().get(0).getData();
    }

}