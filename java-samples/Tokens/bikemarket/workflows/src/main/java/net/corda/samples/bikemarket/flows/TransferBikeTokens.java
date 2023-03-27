package net.corda.samples.bikemarket.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilities;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokensHandler;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow;
import com.r3.corda.lib.tokens.workflows.utilities.NotaryUtilities;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.bikemarket.states.FrameTokenState;
import net.corda.samples.bikemarket.states.WheelsTokenState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.util.Collections;

public class TransferBikeTokens {
    public TransferBikeTokens() {
    }
    @InitiatingFlow
    @StartableByRPC
    public static class TransferBikeToken extends FlowLogic<String> {

        private final String frameSerial;
        private final String wheelsSerial;
        private final Party holder;

        public TransferBikeToken(String frameSerial, String wheelsSerial, Party holder) {
            this.frameSerial = frameSerial;
            this.wheelsSerial = wheelsSerial;
            this.holder = holder;
        }

        @Suspendable
        @Override
        public String call() throws FlowException {
            //Step 1: Frame Token
            //get frame states on ledger
            StateAndRef<FrameTokenState> frameStateAndRef = getServiceHub().getVaultService().
                    queryBy(FrameTokenState.class).getStates().stream()
                    .filter(sf -> sf.getState().getData().getserialNum().equals(this.frameSerial)).findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Frame token with serial = " + this.frameSerial + " not found from vault"));

            //get the TokenType object
            FrameTokenState frametokentype = frameStateAndRef.getState().getData();

            //get the pointer to the frame
            TokenPointer frametokenPointer = frametokentype.toPointer(frametokentype.getClass());

            //Step 2: Wheels Token
            StateAndRef<WheelsTokenState> wheelStateStateAndRef = getServiceHub().getVaultService().
                    queryBy(WheelsTokenState.class).getStates().stream().filter(sf -> sf.getState().getData().getserialNum().equals(this.wheelsSerial)).findAny()
                    .orElseThrow(() -> new IllegalArgumentException("wheel token with serial=" + this.wheelsSerial + " not found from vault"));

            //get the TokenType object
            WheelsTokenState wheeltokentype = wheelStateStateAndRef.getState().getData();

            //get the pointer to the wheel
            TokenPointer wheeltokenPointer = wheeltokentype.toPointer(wheeltokentype.getClass());

            // Obtain a reference to a notary we wish to use.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

            FlowSession sellerSession = initiateFlow(holder);
            TransactionBuilder txBuilder = new TransactionBuilder(notary);
            MoveTokensUtilities.addMoveNonFungibleTokens(txBuilder, getServiceHub(), frametokenPointer, holder);
            MoveTokensUtilities.addMoveNonFungibleTokens(txBuilder, getServiceHub(), wheeltokenPointer, holder);

            SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder);
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, Collections.singletonList(sellerSession)));

            // Update the distribution list
            SignedTransaction ftx =  subFlow(new ObserverAwareFinalityFlow(stx, Collections.singletonList(sellerSession)));

            //Add the new token holder to the distribution list
            subFlow(new UpdateDistributionListFlow(ftx));

            return "\nTransfer ownership of a bike (Frame serial#: "+ this.frameSerial + ", Wheels serial#: " + this.wheelsSerial + ") to "
                    + this.holder.getName().getOrganisation() + "\nTransaction IDs: "
                    + ftx.getId();
        }
    }

    @InitiatedBy(TransferBikeToken.class)
    public static class TransferBikeTokenResponder extends FlowLogic<Void> {

        private FlowSession counterSession;

        public TransferBikeTokenResponder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            //simply use the MoveFungibleTokensHandler as the responding flow
            subFlow(new MoveNonFungibleTokensHandler(counterSession));
            return null;
        }
    }
}