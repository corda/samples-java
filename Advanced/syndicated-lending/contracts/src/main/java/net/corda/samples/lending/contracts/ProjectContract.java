package net.corda.samples.lending.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

// ************
// * Contract *
// ************
public class ProjectContract implements Contract {

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        // Contract Validation Logic Goes Here

    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Issue implements Commands {}
    }
}