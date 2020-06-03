package net.corda.examples.spaceships.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilities;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow;
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler;
import com.r3.corda.lib.tokens.workflows.utilities.NotaryUtilities;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.VaultService;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.examples.spaceships.states.SpaceshipTokenType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The contained flows complete an atomic swap (single transaction exchange between buyer and seller) of a spaceship
 * To achieve this the steps are:
 * 1. Buyer requests the value/sale price of the ship he wants to purchase identified by UUID
 * 2. Seller queries his inventory of ships and grabs the value of the associated UUID and sends to Buyer
 * 3. Buyer checks to see if he has the money to pay - if not throws exception which ends the flow
 * 4. Seller sends the transaction of ownership (the tx that created his ownership token) and sends it to Buyer
 * 5. Buyer creates transaction with the required currency tokens going to seller, and the ship token going to buyer
 * 6. Transaction is signed and finalised
 */
public interface BuySpaceshipFlows {

    @InitiatingFlow
    @StartableByRPC
    class BuySpaceshipInitiator extends FlowLogic<SignedTransaction> {

        private final String shipId;
        private final Party seller;

        public BuySpaceshipInitiator(String shipId, Party seller) {
            this.shipId = shipId;
            this.seller = seller;
        }

        @Suspendable
        @Override
        @SuppressWarnings("unchecked")
        public SignedTransaction call() throws FlowException {
            Party notary = NotaryUtilities.getPreferredNotary(getServiceHub());
            FlowSession sellerSession = initiateFlow(seller);
            VaultService vaultService = getServiceHub().getVaultService();
            boolean processSale = false;
            Amount<TokenType> paymentAmount; // The amount we wish to pay (in any currency - we will check against conversion rates)

            // (STEP 1 - Request Token ship value) see how much the seller's spaceship costs
            Amount<TokenType> shipValue = sellerSession.sendAndReceive(Amount.class, shipId).unwrap(it -> it);

            paymentAmount = shipValue; // first we will try to pay in the native currency of the ship's value

            // Check if we have enough funds to afford this currency and amount
            // tokenBalance returns Amount<TokenType> which represents the quantity of this TokenType (currency) in our vault
            int fundsAvailable = QueryUtilities.tokenBalance(vaultService, shipValue.getToken()).compareTo(paymentAmount);

            if (fundsAvailable >= 0) { // we have enough funds and will go through with the purchase
                processSale = true;
            } else { // we do NOT have enough, in THAT tokenType but check if we can pay in some other currency using exchange rate

                // Creates a set of all tokenTypes we are holding
                Set<TokenType> heldTokenTypes = vaultService.queryBy(FungibleToken.class).getStates().stream()
                        .map(it -> it.getState().getData().getTokenType())
                        .collect(Collectors.toSet());
                heldTokenTypes.remove(shipValue.getToken()); // remove this as it's already been checked

                // Iterate over all the different TokenTypes (currencies) we hold to see if any have enough value against
                // the listed exchange rate (defined in FlowHelpers interface)
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

            /**
             * If the above exception isn't thrown, then we have enough tokens in 'some' currency to complete the sale
             *
             * (Step 2 - receive the transaction which generated the ShipToken and it's associated definition)
             *
             * This token is what the seller holds (the tx proposal will transfer it to the us/buyer) Note: we need the full transaction
             * because it also includes the reference state which the TokenPointer/TokenType refers to - both are required
             * for addMoveNonFungibleTokens when using an EvolvableTokenType.
             */
            SignedTransaction shipTokenTransaction = subFlow(new ReceiveTransactionFlow(sellerSession));

            /**
             * We record the transaction in OUR vault so that the TransactionBuilder can access it.
             * Important: make sure you have the argument StatesToRecord.ALL_VISIBLE as the default recordTransactions will only
             * record states in the transaction which are RELEVANT (i.e. which we participated in) this is not 'our' transaction
             * so we need that argument ALL_VISIBLE.
             */
            getServiceHub().recordTransactions(StatesToRecord.ALL_VISIBLE, Collections.singletonList(shipTokenTransaction));
            NonFungibleToken shipNFT = shipTokenTransaction.getCoreTransaction().outputsOfType(NonFungibleToken.class).get(0);
            TokenPointer<SpaceshipTokenType> shipTokenPointer = (TokenPointer<SpaceshipTokenType>) shipNFT.getTokenType();

            // Gather the paymentAmount in Tokens on our side (the tx proposal will transfer these to the seller)
            Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> selectedTokens = new DatabaseTokenSelection(getServiceHub())
                    // here we are generating input and output states which send the correct amount to the seller, and any change back to buyer
                    .generateMove(Collections.singletonList(new Pair<>(seller, paymentAmount)), getOurIdentity());

            // Build the transaction which transfers the currency tokens AND the spaceship token, in a single transaction
            TransactionBuilder txBuilder = new TransactionBuilder(notary);
            MoveTokensUtilities.addMoveNonFungibleTokens(txBuilder, getServiceHub(), shipTokenPointer, getOurIdentity());
            MoveTokensUtilities.addMoveTokens(txBuilder, selectedTokens.getFirst(), selectedTokens.getSecond());

            SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder);
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, Collections.singletonList(sellerSession)));

            // Update the distribution list
            subFlow(new UpdateDistributionListFlow(stx));
            return subFlow(new ObserverAwareFinalityFlow(stx, Collections.singletonList(sellerSession)));
        }
    }

    @InitiatedBy(BuySpaceshipInitiator.class)
    class BuySpaceshipResponder extends FlowLogic<Void> {
        private final FlowSession counterpartySession;

        public BuySpaceshipResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // Receive request for value of the given shipId
            String shipId = counterpartySession.receive(String.class).unwrap(it -> it);
            UUID shipUUID = UUID.fromString(shipId);

            // Get the state definition from vault to grab the value
            SpaceshipTokenType spaceshipTokenType = FlowHelpers.uuidToSpaceShipTokenType(getServiceHub().getVaultService(), shipUUID);

            // (Step 1 - Respond with value) send back value
            counterpartySession.send(spaceshipTokenType.getValue());

            StateAndRef<NonFungibleToken> spaceshipNFTStateAndRef = QueryUtilities.heldTokensByToken(getServiceHub().getVaultService(), spaceshipTokenType.toPointer())
                    .getStates().get(0);

            // (Step 2 - Send the corresponding TX representing our ownership so the buyer can build and propose the full transaction)
            SignedTransaction shipTokenTransaction = getServiceHub().getValidatedTransactions().getTransaction(spaceshipNFTStateAndRef.getRef().getTxhash());
            assert shipTokenTransaction != null;
            subFlow(new SendTransactionFlow(counterpartySession, shipTokenTransaction));

            SignedTransaction stx = subFlow(new SignTransactionFlow(counterpartySession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // No checks for simplicity - but here you can check that you will actually receive
                    // the correct amount of tokens before signing (among other possible checks).
                }
            });

            subFlow(new ObserverAwareFinalityFlowHandler(counterpartySession));
            return null;
        }
    }
}
