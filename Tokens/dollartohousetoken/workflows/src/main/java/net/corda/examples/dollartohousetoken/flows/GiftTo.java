package net.corda.examples.dollartohousetoken.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// ******************
// * Initiator flow *
// ******************

@InitiatingFlow
@StartableByRPC
public class GiftTo extends FlowLogic<SignedTransaction> {

    // We will not use these ProgressTracker for this Hello-World sample
        private final ProgressTracker progressTracker = new ProgressTracker();
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        //private variables
        private Party sender ;
        private Party receiver;
        private int amount;

        //public constructor
        public GiftTo(Party giftTo, int amount){
            this.receiver = giftTo;
            this.amount = amount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            //Hello World message
            String msg = "Hello-World";
            this.sender = getOurIdentity();

            // Step 1. Get a reference to the notary service on our network and our key pair.
            // Note: ongoing work to support multiple notary identities is still in progress.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            /* Create instance of the fiat currency token amount */
            Amount<TokenType> priceToken = new Amount<>(this.amount, FiatCurrency.Companion.getInstance("USD"));
           return subFlow(new MoveFungibleTokens(priceToken,this.receiver));
        }
}



