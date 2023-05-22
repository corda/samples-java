package net.corda.samples.auction.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilities;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.sun.istack.NotNull;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.auction.states.Asset;
import net.corda.samples.auction.states.AuctionState;

import java.util.*;

/**
 * This flows takes care of the delivery-vs-payment for settlement of the auction. The auctioned asset's ownership is
 * transferred from the auctioneer to the highest bidder and the bid amount is transferred from the highest bidder to
 * the auctioneer in a single atomic transaction.
 */
public class AuctionDvPFlow {

    @StartableByRPC
    @InitiatingFlow
    public static class AuctionDvPInitiator extends FlowLogic<SignedTransaction>{

        private final UUID auctionId;
        private final Amount<Currency> payment;

        /**
         * Constructor to initialise flows parameters.
         *
         * @param auctionId is the unique id of the auction to be settled
         * @param payment is the bid amount which is required to be transferred from the highest bidded to auctioneer to
         * settle the auction.
         */
        public AuctionDvPInitiator(UUID auctionId, Amount<Currency> payment) {
            this.auctionId = auctionId;
            this.payment = payment;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Query the vault to fetch a list of all AuctionState states, and filter the results based on the auctionId
            // to fetch the desired AuctionState states from the vault.
            List<StateAndRef<AuctionState>> auctionStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AuctionState.class).getStates();
            StateAndRef<AuctionState> auctionStateAndRef = auctionStateAndRefs.stream().filter(stateAndRef -> {
                AuctionState auctionState = stateAndRef.getState().getData();
                return auctionState.getAuctionId().equals(auctionId);
            }).findAny().orElseThrow(() -> new FlowException("Auction Not Found"));
            AuctionState auctionState = auctionStateAndRef.getState().getData();
            Amount<Currency> winningBidPrice = auctionState.getWinningBid();

            // Create a QueryCriteria to query the Asset.
            // Resolve the linear pointer in previously filtered auctionState to fetch the assetState containing
            // the asset's unique id.
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                    null, Arrays.asList(auctionStateAndRef.getState().getData().getAuctionItem()
                    .resolve(getServiceHub()).getState().getData().getLinearId().getId()),
                    null, Vault.StateStatus.UNCONSUMED);

            // Use the vaultQuery with the previously created queryCriteria to fetch th assetState to be used as input
            // in the transaction.
            StateAndRef<Asset> assetStateAndRef = getServiceHub().getVaultService().
                    queryBy(Asset.class, queryCriteria).getStates().get(0);

            // Use the withNewOwner() of the Ownable states get the command and the output states to be used in the
            // transaction from ownership transfer of the asset.
            CommandAndState commandAndState = assetStateAndRef.getState().getData()
                    .withNewOwner(auctionState.getWinner());

            // Start a flow session with the auctioneer
            FlowSession counterpartySession = initiateFlow(auctionState.getAuctioneer());

            // Obtain a reference to a notary we wish to use.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = auctionStateAndRef.getState().getNotary();

            // Create the transaction builder.
            TransactionBuilder transactionBuilder = new TransactionBuilder(notary);

            // Create an instance of the fiat currency token amount
            Amount<TokenType> winningBidToken = new Amount<>(winningBidPrice.getQuantity(), FiatCurrency.Companion.getInstance(winningBidPrice.getToken().getCurrencyCode()));

            // Generate the move proposal - this returns input/output pair for the fiat currency transfer
            PartyAndAmount<TokenType> partyAndAmount = new PartyAndAmount<>(counterpartySession.getCounterparty(), winningBidToken);
            Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs = new DatabaseTokenSelection(getServiceHub())
                    .generateMove(Collections.singletonList(new Pair<>(counterpartySession.getCounterparty(), winningBidToken)), auctionState.getWinner());

            // Create a fiat currency proposal for the asset using the helper function provided by TokenSDK
            MoveTokensUtilities.addMoveTokens(transactionBuilder, inputsAndOutputs.getFirst(), inputsAndOutputs.getSecond());

            // Update the transaction builder with the input and output for the asset's ownership transfer.
            transactionBuilder.addInputState(assetStateAndRef)
                    .addOutputState(commandAndState.getOwnableState())
                    .addCommand(commandAndState.getCommand(),
                            Arrays.asList(auctionState.getAuctioneer().getOwningKey()));

            // Verify the transaction
            transactionBuilder.verify(getServiceHub());

            SignedTransaction partiallySignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTransaction, Arrays.asList(counterpartySession)));

            return subFlow(new FinalityFlow(signedTransaction, (counterpartySession)));
        }
    }

    @InitiatedBy(AuctionDvPInitiator.class)
    public static class AuctionDvPResponder extends FlowLogic<SignedTransaction>{

        private FlowSession otherPartySession;

        public AuctionDvPResponder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            subFlow(new SignTransactionFlow(otherPartySession) {

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // Additional Checks here
                }
            });
            return subFlow(new ReceiveFinalityFlow(otherPartySession));
        }
    }
}
