package net.corda.samples.statereissuance.contracts;

import net.corda.core.contracts.Command;
import net.corda.samples.statereissuance.states.LandTitleState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class LandTitleContract implements Contract {

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        if(tx.getCommands().size() == 0){
            throw new IllegalArgumentException("One command Expected");
        }

        Command command = tx.getCommand(0);
        if(command.getValue() instanceof Commands.Issue)
            verifyIssue(tx);

        else if(command.getValue() instanceof Commands.Transfer)
            verifyTransfer(tx);

        else if (command.getValue() instanceof Commands.Exit)
            verifyExit(tx);
    }

    private void verifyIssue(LedgerTransaction tx){
        // Land Title Issue Contract Verification Logic goes here
        if(tx.getOutputStates().size() != 1) throw new IllegalArgumentException("One Output Expected");

        Command command = tx.getCommand(0);
        if(!(command.getSigners().contains(((LandTitleState)tx.getOutput(0)).getIssuer().getOwningKey())))
            throw new IllegalArgumentException("Issuer Signature Required");
    }

    private void verifyTransfer(LedgerTransaction tx){
        // Land Title Transfer Contract Verification Logic goes here
        Command command = tx.getCommand(0);
        LandTitleState landTitleState = (LandTitleState) tx.getInput(0);
        if (!(command.getSigners().contains(landTitleState.getIssuer().getOwningKey())) &&
                (landTitleState.getOwner() != null
                        && command.getSigners().contains(landTitleState.getOwner().getOwningKey())))
            throw new IllegalArgumentException("Issuer and Owner must Sign");
    }

    private void verifyExit(LedgerTransaction tx){
        // Land Title Exit Contract Verification Logic goes here

        if(tx.getOutputStates().size() != 0) throw new IllegalArgumentException("Zero Output Expected");
        if(tx.getInputStates().size() != 1) throw new IllegalArgumentException("One Input Expected");

        Command command = tx.getCommand(0);
        if(!(command.getSigners().contains(((LandTitleState)tx.getInput(0)).getIssuer().getOwningKey())))
            throw new IllegalArgumentException("Issuer Signature Required");
    }


    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Issue implements Commands {}
        class Transfer implements Commands {}
        class Exit implements Commands {}
        class Reissue implements Commands {}
    }
}