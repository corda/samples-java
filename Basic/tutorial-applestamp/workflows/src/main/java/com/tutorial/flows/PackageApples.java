package com.tutorial.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.tutorial.contracts.BasketOfApplesContract;
import com.tutorial.states.BasketOfApples;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Collections;
import net.corda.core.identity.CordaX500Name;

public class PackageApples {

    @InitiatingFlow
    @StartableByRPC
    public static class PackApplesInitiator extends FlowLogic<SignedTransaction> {

        private String appleDescription;
        private int weight;

        public PackApplesInitiator(String appleDescription, int weight) {
            this.appleDescription = appleDescription;
            this.weight = weight;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Obtain a reference to a notary we wish to use.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

            //Create the output object
            BasketOfApples basket = new BasketOfApples(this.appleDescription,this.getOurIdentity(),this.weight);

            //Building transaction
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(basket)
                    .addCommand(new BasketOfApplesContract.Commands.packBasket(), this.getOurIdentity().getOwningKey());

            // Verify the transaction
            txBuilder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

            // Notarise the transaction and record the states in the ledger.
            return subFlow(new FinalityFlow(signedTransaction, Collections.emptyList()));
        }
    }

}

//flow start PackApplesInitiator appleDescription: Fuji4072, weight: 10
//run vaultQuery contractStateType: com.tutorial.states.BasketOfApples