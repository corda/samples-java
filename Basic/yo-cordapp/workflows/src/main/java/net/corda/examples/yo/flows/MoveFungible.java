package net.corda.examples.yo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler;
import kotlin.Unit;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.yo.states.YoTokenFungible;

import java.util.Arrays;
import java.util.UUID;


/**
 * Designed initiating node : Company
 * This flow is designed for company to move the issued tokens of stock to the a shareholder node.
 * To make it more real, we can modify it such that the shareholder exchanges some fiat currency for some stock tokens.
 */
@InitiatingFlow
@StartableByRPC
public class MoveFungible {

    @InitiatingFlow
    @StartableByRPC
    public static class MoveFungibleInitiator extends FlowLogic<String> {
        private final String tokenId;
        private final Long quantity;
        private final Party recipient;

        public MoveFungibleInitiator(String tokenId, Long quantity, Party recipient) {
            this.tokenId = tokenId;
            this.quantity = quantity;
            this.recipient = recipient;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {

            /* Get the UUID from the houseId parameter */
            UUID uuid = UUID.fromString(tokenId);

            /* Fetch the house state from the vault using the vault query */
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                    null, Arrays.asList(uuid), null, Vault.StateStatus.UNCONSUMED);

            StateAndRef<YoTokenFungible> yoFungibleStateAndRef = getServiceHub().getVaultService().
                    queryBy(YoTokenFungible.class, queryCriteria).getStates().get(0);

            YoTokenFungible yoState = yoFungibleStateAndRef.getState().getData();

            // With the pointer, we can get the create an instance of transferring Amount
            Amount<TokenType> amount = new Amount(quantity, yoState.toPointer());

            //Use built-in flow for move tokens to the recipient
            SignedTransaction stx = subFlow(new MoveFungibleTokens(amount, recipient));

            return "\nMove "+this.quantity +" Fungible Yo Tokens to "
                    + this.recipient.getName().getOrganisation() + ".\nTransaction ID: "+stx.getId();

        }
    }

    @InitiatedBy(MoveFungibleInitiator.class)
    public static class MoveFungibleResponder extends FlowLogic<Unit>{

        private FlowSession counterSession;

        public MoveFungibleResponder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            // Simply use the MoveFungibleTokensHandler as the responding flow
            return subFlow(new MoveFungibleTokensHandler(counterSession));
        }
    }
}