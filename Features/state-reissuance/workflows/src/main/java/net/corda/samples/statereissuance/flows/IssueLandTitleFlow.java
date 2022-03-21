package net.corda.samples.statereissuance.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.statereissuance.contracts.LandTitleContract;
import net.corda.samples.statereissuance.states.LandTitleState;

import java.util.Arrays;

public class IssueLandTitleFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction>{

        //private variables
        private Party owner;
        private String dimensions;
        private String area;


        //public constructor
        public Initiator(Party owner,String dimensions, String area) {
            this.owner = owner;
            this.dimensions = dimensions;
            this.area = area;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Get a reference to the notary service on our network and our key pair.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
            Party issuer = getOurIdentity();

            LandTitleState landTitleState = new LandTitleState(new UniqueIdentifier(), dimensions, area, owner, issuer);

            final TransactionBuilder builder = new TransactionBuilder(notary)
                    .addOutputState(landTitleState)
                    .addCommand(new LandTitleContract.Commands.Issue(), Arrays.asList(issuer.getOwningKey()));

            // Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(ptx, Arrays.asList(initiateFlow(owner))));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void>{
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession));
            return null;
        }
    }

}