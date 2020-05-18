package net.corda.examples.spaceships.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.spaceships.states.SpaceshipTokenState;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public interface BuySpaceShipFlows {

    @InitiatingFlow
    @StartableByRPC
    class BuyUniqueSpaceshipInitiator extends FlowLogic<SignedTransaction> {

        private final String shipId;
        private final Party seller;

        public BuyUniqueSpaceshipInitiator(String shipId, Party seller) {
            this.shipId = shipId;
            this.seller = seller;
        }

        @Suspendable
        @Override
        @SuppressWarnings("unchecked")
        public SignedTransaction call() throws FlowException {
            FlowSession sellerSession = initiateFlow(seller);
            VaultService vaultService = getServiceHub().getVaultService();
            boolean processSale = false;
            Amount<TokenType> paymentAmount;

            // see how much the seller's spaceship costs
            Amount<TokenType> shipValue = sellerSession.sendAndReceive(Amount.class, shipId).unwrap(it -> it);

            // check if we have this currency and amount
            // compareTo will return >= 0 if we have enough
            paymentAmount = shipValue;
            int fundsAvailable = QueryUtilities.tokenBalance(vaultService, shipValue.getToken()).compareTo(paymentAmount);

            if (fundsAvailable >= 0) {
                processSale = true;
            } else { // check if we can pay in some other currency using exchange rate

                // creates a set (unique) of all tokenTypes we are holding
                Set<TokenType> heldTokenTypes = vaultService.queryBy(FungibleToken.class).getStates().stream()
                        .map(it -> it.getState().getData().getTokenType())
                        .collect(Collectors.toSet());
                heldTokenTypes.remove(shipValue.getToken()); // remove this as it's already been checked

                for (TokenType currentTokenType : heldTokenTypes) {
                    paymentAmount = FlowHelpers.exchangeCurrency(shipValue, currentTokenType);
                    int funds = QueryUtilities.tokenBalance(vaultService, currentTokenType).compareTo(paymentAmount);
                    if (funds >= 0) {
                        processSale = true;
                        break;
                    }
                }
            }

            if (!processSale) throw new FlowException("Insufficient Funds to Buy the spaceship " + shipId);

            // We have enough, initiate the sale

            return null;
        }
    }

    @InitiatedBy(BuyUniqueSpaceshipInitiator.class)
    class BuyUniqueSpaceshipResponder extends FlowLogic<Void> {
        private final FlowSession counterpartySession;

        public BuyUniqueSpaceshipResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // receive request for value of the given shipId
            String shipId = counterpartySession.receive(String.class).unwrap(it -> it);
            UUID shipUUID = UUID.fromString(shipId);

            // Get the state definition from vault to grab the value
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, Collections.singletonList(shipUUID));
            StateAndRef<SpaceshipTokenState> spaceShip = getServiceHub().getVaultService()
                    .queryBy(SpaceshipTokenState.class, queryCriteria).getStates().get(0);

            SpaceshipTokenState spaceshipTokenState = spaceShip.getState().getData();

            // send back value
            counterpartySession.send(spaceshipTokenState.getValue());

            return null;
        }
    }
}
