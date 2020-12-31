package net.corda.samples.tokenizedhouse.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import net.corda.core.contracts.TransactionState;
import net.corda.samples.tokenizedhouse.states.FungibleHouseTokenState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.utilities.ProgressTracker;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryTokens {

    @InitiatingFlow
    @StartableByRPC
    public static class GetTokenBalance extends FlowLogic<String> {
        private final ProgressTracker progressTracker = new ProgressTracker();
        private final String symbol;

        public GetTokenBalance(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            //get a set of the RealEstateEvolvableTokenType object on ledger with uuid as input tokenId
            Set<FungibleHouseTokenState> evolvableTokenTypeSet = getServiceHub().getVaultService().
                    queryBy(FungibleHouseTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).map(StateAndRef::getState)
                    .map(TransactionState::getData).collect(Collectors.toSet());
            if (evolvableTokenTypeSet.isEmpty()){
                throw new IllegalArgumentException("FungibleHouseTokenState symbol=\""+symbol+"\" not found from vault");
            }

            // Save the result
            String result="";

            // Technically the set will only have one element, because we are query by symbol.
            for (FungibleHouseTokenState evolvableTokenType : evolvableTokenTypeSet){
                //get the pointer pointer to the house
                TokenPointer<FungibleHouseTokenState> tokenPointer = evolvableTokenType.toPointer(FungibleHouseTokenState.class);
                //query balance or each different Token
                Amount<TokenType> amount = QueryUtilities.tokenBalance(getServiceHub().getVaultService(), tokenPointer);
                result += "\nYou currently have "+ amount.getQuantity()+ " " + symbol + " Tokens issued by "
                        +evolvableTokenType.getMaintainer().getName().getOrganisation()+"\n";
            }
            return result;
        }
    }

}