package net.corda.examples.spaceships.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import com.r3.corda.lib.tokens.workflows.utilities.NonFungibleTokenBuilder;
import com.r3.corda.lib.tokens.workflows.utilities.NotaryUtilities;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.TransactionState;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.spaceships.states.SpaceshipTokenType;

import java.util.Collections;

import static net.corda.examples.spaceships.flows.FlowHelpers.parseAmountFromString;

/**
 * The contained flows are initiated from the perspective of a manufacturer (initiating node)
 * - the initiating node can bring a Spaceship token on ledger and assign it to a holder
 * - the manufacturer (initiating node) is designated as a 'maintainer' (a party that can update the tokenDefinition)
 */
public interface IssueSpaceshipFlows {

    /**
     * A Fungible Spaceship is one whose tokens can be split or merged and allows fractional ownership
     * - instances of this type of token will be used for investment / group ownership of a spaceship
     */
    @StartableByRPC
    class TokenizeFungibleSpaceship extends FlowLogic<SignedTransaction> {
        private final Party holder;
        private final String model;
        private final String planetOfOrigin;
        private final int seatingCapacity;
        private final Amount<TokenType> value;
        private final int tokenAmount;

        public TokenizeFungibleSpaceship(Party holder, String model, String planetOfOrigin, int seatingCapacity, Amount<TokenType> value, int tokenAmount) {
            this.holder = holder;
            this.model = model;
            this.planetOfOrigin = planetOfOrigin;
            this.seatingCapacity = seatingCapacity;
            this.value = value;
            this.tokenAmount = tokenAmount;
        }

        // Overload for simple value
        public TokenizeFungibleSpaceship(Party holder, String model, String planetOfOrigin, int seatingCapacity, String value, int tokenAmount) {
            this.holder = holder;
            this.model = model;
            this.planetOfOrigin = planetOfOrigin;
            this.seatingCapacity = seatingCapacity;
            this.value = parseAmountFromString(value); // value format: 'int <CURR CODE>' ie. '100 USD'
            this.tokenAmount = tokenAmount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final Party manufacturer = getOurIdentity(); // node that tokenizes assumed to be manufacturer

            // NotaryUtilities allows you to set a strategy for notary selection - otherwise you can default to firstnotary, or random
            final Party notary = NotaryUtilities.getPreferredNotary(getServiceHub());

            /**
             * Create the evolvableTokenState
             *
             * CreateEvolvableTokens() will behind the scenes commit our token definition to the ledgers
             * of any maintainers specified in the SpaceshipTokenState (in our case the manufacturer) as well as any observers we pass in as an
             * optional argument - we have not passed in any observers to CreateEvolvableTokens()
             */
            SpaceshipTokenType evolvableSpaceshipToken = new SpaceshipTokenType(manufacturer, model, planetOfOrigin, seatingCapacity, value, true);
            TransactionState<SpaceshipTokenType> txState = new TransactionState<>(evolvableSpaceshipToken, notary);
            subFlow(new CreateEvolvableTokens(txState));


            /** Build our FungibleToken and issue it to the holder - Note: Because we used EvolvableTokenType as our base, we will be issuing
             * TokenPointer which is of type TokenType and can be resolved to the definition. This allows the definition of the token to evolve
             * independently of who is holding it. The holder can reference the current definition at anytime and will receive updates when it is changed.
             */
            TokenPointer<SpaceshipTokenType> evolvableSpaceshipTokenPtr = evolvableSpaceshipToken.toPointer();

            // The FungibleTokenBuilder allows quick and easy stepwise assembly of a token that can be split/merged
            FungibleToken token = new FungibleTokenBuilder()
                    .ofTokenType(evolvableSpaceshipTokenPtr)
                    .withAmount(tokenAmount)
                    .issuedBy(manufacturer)
                    .heldBy(holder)
                    .buildFungibleToken();

            return subFlow(new IssueTokens(Collections.singletonList(token)));
        }
    }

    /**
     * A NonFungible Spaceship is one where there is only ONE holder (it can not be split or merged)
     * - instances of this type of token will be used for ownership of UNIQUE spaceships.
     */
    @StartableByRPC
    class TokenizeNonFungibleSpaceship extends FlowLogic<SignedTransaction> {
        private final Party holder;
        private final String model;
        private final String planetOfOrigin;
        private final int seatingCapacity;
        private final Amount<TokenType> value;

        public TokenizeNonFungibleSpaceship(Party holder, String model, String planetOfOrigin, int seatingCapacity, Amount<TokenType> value) {
            this.holder = holder;
            this.model = model;
            this.planetOfOrigin = planetOfOrigin;
            this.seatingCapacity = seatingCapacity;
            this.value = value;
        }

        // Overload for simple value
        public TokenizeNonFungibleSpaceship(Party holder, String model, String planetOfOrigin, int seatingCapacity, String value) {
            this.holder = holder;
            this.model = model;
            this.planetOfOrigin = planetOfOrigin;
            this.seatingCapacity = seatingCapacity;
            this.value = parseAmountFromString(value); // value format: 'int <CURR CODE>' ie. '100 USD'
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final Party manufacturer = getOurIdentity(); // node that tokenizes assumed to be manufacturer

            // NotaryUtilities allows you to set a strategy for notary selection - otherwise you can default to first notary, or random
            final Party notary = NotaryUtilities.getPreferredNotary(getServiceHub());

            /**
             * Create the evolvableTokenState
             *
             * CreateEvolvableTokens() will behind the scenes commit our token definition to the ledgers
             * of the any maintainers specified in the SpaceshipTokenState (in our case the manufacturer) as well as any observers we pass in as an
             * optional argument - we have not passed in any observers to CreateEvolvableTokens()
             */
            SpaceshipTokenType evolvableSpaceshipToken = new SpaceshipTokenType(manufacturer, model, planetOfOrigin, seatingCapacity, value, false);
            TransactionState<SpaceshipTokenType> txState = new TransactionState<>(evolvableSpaceshipToken, notary);
            subFlow(new CreateEvolvableTokens(txState));


            /** Build our FungibleToken and issue it to the holder - Note: Because we used EvolvableTokenType as our base, we will be issuing
             * TokenPointer which is of type TokenType and can be resolved to the definition. This allows the definition of the token to evolve
             * independently of who is holding it. The holder can reference the current definition at anytime and will receive updates when it is changed.
             */
            TokenPointer<SpaceshipTokenType> evolvableSpaceshipTokenPtr = evolvableSpaceshipToken.toPointer();

            // The NonFungibleTokenBuilder allows quick and easy stepwise assembly of a token that can only be held outright (no associated amount)
            // Notice that when building a NonFungibleToken, you do not add an 'amount'
            NonFungibleToken token = new NonFungibleTokenBuilder()
                    .ofTokenType(evolvableSpaceshipTokenPtr)
                    .issuedBy(manufacturer)
                    .heldBy(holder)
                    .buildNonFungibleToken();

            return subFlow(new IssueTokens(Collections.singletonList(token)));
        }
    }
}
