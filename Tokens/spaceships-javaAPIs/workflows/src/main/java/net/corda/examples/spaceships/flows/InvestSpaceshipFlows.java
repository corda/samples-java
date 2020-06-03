package net.corda.examples.spaceships.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.spaceships.states.SpaceshipTokenType;

import java.math.BigDecimal;
import java.util.UUID;

public interface InvestSpaceshipFlows {

    /**
     * Queries how many shares a seller has to sell, and cost/share
     */
    @StartableByRPC
    @InitiatingFlow
    class sharesOwnedInSpaceshipInitiator extends FlowLogic<Pair<BigDecimal, Amount<TokenType>>> {

        private final String shipId;
        private final Party seller;

        public sharesOwnedInSpaceshipInitiator(String shipId, Party seller) {
            this.shipId = shipId;
            this.seller = seller;
        }

        @Suspendable
        @Override
        @SuppressWarnings("unchecked")
        public Pair<BigDecimal, Amount<TokenType>> call() throws FlowException {
            FlowSession sellerSession = initiateFlow(seller);

            Pair<Amount<TokenType>, Amount<TokenType>> sharesAndValue = sellerSession.sendAndReceive(Pair.class, shipId).unwrap(it -> it);
            return new Pair<>(sharesAndValue.getFirst().toDecimal() , sharesAndValue.getSecond());
        }
    }

    @InitiatedBy(sharesOwnedInSpaceshipInitiator.class)
    class sharesOwnedInSpaceshipResponder extends FlowLogic<Void> {

        private final FlowSession counterpartySession;

        public sharesOwnedInSpaceshipResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // Receive request for value of the given shipId
            String shipId = counterpartySession.receive(String.class).unwrap(it -> it);
            UUID shipUUID = UUID.fromString(shipId);

            SpaceshipTokenType spaceshipTokenType = FlowHelpers.uuidToSpaceShipTokenType(getServiceHub().getVaultService(), shipUUID);

            Amount<TokenType> amountOfSpaceShipTokens = QueryUtilities.tokenBalance(getServiceHub().getVaultService(), spaceshipTokenType.toPointer());

            counterpartySession.send(new Pair<>(amountOfSpaceShipTokens, spaceshipTokenType.getValue()));
            return null;
        }
    }

    /**
     * Initiates a BUY of X number of shares of a particular spaceship (shipId)
     * The total price is the SpaceshipTokenType value * amount being bought
     */
    @StartableByRPC
    @InitiatingFlow
    class BuySharesInSpaceshipInitiator extends FlowLogic<SignedTransaction> {

        private final long numShares;
        private final Amount<TokenType> paymentAmount;
        private final String shipId;
        private final Party seller;

        public BuySharesInSpaceshipInitiator(Double numShares, Amount<TokenType> paymentAmount, String shipId, Party seller) {
            this.numShares = numShares.longValue();
            this.paymentAmount = paymentAmount;
            this.shipId = shipId;
            this.seller = seller;
        }

        // Overload for BigDecimal shares
        public BuySharesInSpaceshipInitiator(BigDecimal numShares, Amount<TokenType> paymentAmount, String shipId, Party seller) {
            this(numShares.doubleValue(), paymentAmount, shipId, seller);
        }

        // Overload for simple amount
        public BuySharesInSpaceshipInitiator(Double numShares, String paymentAmount, String shipId, Party seller) {
            this(numShares, FlowHelpers.parseAmountFromString(paymentAmount), shipId, seller);
        }

        // Overload for simple amount and BigDecimal shares
        public BuySharesInSpaceshipInitiator(BigDecimal numShares, String paymentAmount, String shipId, Party seller) {
            this(numShares.doubleValue(), FlowHelpers.parseAmountFromString(paymentAmount), shipId, seller);
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            FlowSession sellerSession = initiateFlow(seller);
            // Notify seller which ship you want and how many shares
            sellerSession.send(new Pair<>(shipId, numShares));

            SignedTransaction paymentStx = subFlow(new MoveFungibleTokens(new PartyAndAmount<>(seller, paymentAmount)));
            if (paymentStx != null) sellerSession.send(paymentStx.getId());
            return subFlow(new ReceiveTransactionFlow(sellerSession));
        }
    }

    @InitiatedBy(BuySharesInSpaceshipInitiator.class)
    class BuySharesInSpaceshipResponder extends FlowLogic<Void> {

        private final FlowSession counterpartySession;

        public BuySharesInSpaceshipResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        @SuppressWarnings("unchecked")
        public Void call() throws FlowException {
            Pair<String, Long> shipIdAndShares = counterpartySession.receive(Pair.class).unwrap(it -> it);
            UUID shipUUID = UUID.fromString(shipIdAndShares.getFirst());

            // Calculated expected amount you will receive from the buyer
            SpaceshipTokenType shipForSale = FlowHelpers.uuidToSpaceShipTokenType(getServiceHub().getVaultService(), shipUUID);
            Amount<TokenType> amountOwed = shipForSale.getValue().times(shipIdAndShares.getSecond());

            SecureHash paymentId = counterpartySession.receive(SecureHash.class).unwrap(it -> it);

            // Check if we have received the proper funds in our vault already
            SignedTransaction paymentTx = getServiceHub().getValidatedTransactions().getTransaction(paymentId);
            assert paymentTx != null;
            Amount<IssuedTokenType> amountReceived = ((FungibleToken) paymentTx.getCoreTransaction().getOutput(0)).getAmount();

            if (!(amountOwed.getQuantity() == amountReceived.getQuantity()) || !(amountOwed.getToken().getTokenIdentifier()
                    .equals(amountReceived.getToken().getTokenIdentifier())))
                throw new FlowException("Incorrect funds or currency received for purchase of ship shares");

            // Deliver the shares to the buyer
            // rescale numShares to Long representation with fractional digit precision
            long numShares = (long) (shipIdAndShares.getSecond() *Math.pow(10, shipForSale.getFractionDigits()));
            Amount<TokenType> amountOfShares = new Amount<>(numShares, shipForSale.toPointer());

            SignedTransaction shipFulfilmentTx = subFlow(new MoveFungibleTokens(new PartyAndAmount<>(counterpartySession.getCounterparty(), amountOfShares)));
            subFlow(new SendTransactionFlow(counterpartySession, shipFulfilmentTx));
            return null;
        }
    }
}
