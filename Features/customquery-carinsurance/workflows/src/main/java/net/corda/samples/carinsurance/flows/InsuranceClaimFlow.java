package net.corda.samples.carinsurance.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.internal.FetchDataFlow;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.FieldInfo;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.Sort;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.carinsurance.contracts.InsuranceContract;
import net.corda.samples.carinsurance.schema.InsuranceSchemaV1;
import net.corda.samples.carinsurance.schema.PersistentInsurance;
import net.corda.samples.carinsurance.states.Claim;
import net.corda.samples.carinsurance.states.InsuranceState;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class InsuranceClaimFlow {

    private InsuranceClaimFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class InsuranceClaimInitiator extends FlowLogic<SignedTransaction>{

        private final ClaimInfo claimInfo;
        private final String policyNumber;

        public InsuranceClaimInitiator(ClaimInfo claimInfo, String policyNumber) {
            this.claimInfo = claimInfo;
            this.policyNumber = policyNumber;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException{
            // Query the vault to fetch list of all Insurance state based on the policy number.
            // This state would be used as input to the
            // transaction.
            QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Field valueField = null;
            try{
                valueField = PersistentInsurance.class.getDeclaredField("policyNumber");
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            CriteriaExpression criteria = Builder.equal(valueField, policyNumber);
            QueryCriteria insuranceQuery = new QueryCriteria.VaultCustomQueryCriteria(criteria);
            generalCriteria = generalCriteria.and(insuranceQuery);

            Vault.Page<InsuranceState> insuranceStateAndRefs;
            insuranceStateAndRefs = getServiceHub().getVaultService().queryBy(InsuranceState.class, generalCriteria );
            if(insuranceStateAndRefs.getStates().isEmpty()){
                throw new IllegalArgumentException("Policy not found");
            }

            StateAndRef<InsuranceState> inputStateAndRef = insuranceStateAndRefs.getStates().get(0);
            Claim claim = new Claim(claimInfo.getClaimNumber(), claimInfo.getClaimDescription(),
                    claimInfo.getClaimAmount());
            InsuranceState input = inputStateAndRef.getState().getData();

            List<Claim> claims = new ArrayList<>();
            if(input.getClaims() == null || input.getClaims().size() == 0 ){
                claims.add(claim);
            }else {
                claims.addAll(input.getClaims());
                claims.add(claim);
            }

            //Create the output state
            InsuranceState output = new InsuranceState(input.getPolicyNumber(), input.getInsuredValue(),
                    input.getDuration(), input.getPremium(), input.getInsurer(), input.getInsuree(),
                    input.getVehicleDetail(), claims);

            // Build the transaction.
            TransactionBuilder transactionBuilder = new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addOutputState(output, InsuranceContract.ID)
                    .addCommand(new InsuranceContract.Commands.AddClaim(), ImmutableList.of(getOurIdentity().getOwningKey()));

            // Verify the transaction
            transactionBuilder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            // Call finality Flow
            FlowSession counterpartySession = initiateFlow(input.getInsuree());
            return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(counterpartySession)));

        }
    }


    @InitiatedBy(InsuranceClaimInitiator.class)
    public static class InsuranceClaimResponder extends FlowLogic<SignedTransaction> {

        private FlowSession counterpartySession;

        public InsuranceClaimResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }
}
