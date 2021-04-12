package net.corda.samples.businessmembership.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.bn.flows.BNService;
import net.corda.bn.flows.IllegalMembershipStatusException;
import net.corda.bn.flows.MembershipAuthorisationException;
import net.corda.bn.flows.MembershipNotFoundException;
import net.corda.bn.states.BNRole;
import net.corda.bn.states.MembershipState;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.businessmembership.contracts.InsuranceStateContract;
import net.corda.samples.businessmembership.states.CareProviderIdentity;
import net.corda.samples.businessmembership.states.InsuranceState;
import net.corda.samples.businessmembership.states.InsurerIdentity;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IssuePolicy {

    @InitiatingFlow
    @StartableByRPC
    public static class IssuePolicyInitiator extends FlowLogic<SignedTransaction> {

        private String networkId;
        private Party careProvider;
        private String insuree;

        public IssuePolicyInitiator(String networkId, Party careProvider, String insuree) {
            this.networkId = networkId;
            this.careProvider = careProvider;
            this.insuree = insuree;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            businessNetworkFullVerification(this.networkId, getOurIdentity(), this.careProvider);
            InsuranceState outputState = new InsuranceState(getOurIdentity(), this.insuree, this.careProvider, networkId, "Initiating Policy");
            BNService bnService = getServiceHub().cordaService(BNService.class);
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(outputState)
                    .addCommand(new InsuranceStateContract.Commands.Issue(), Arrays.asList(getOurIdentity().getOwningKey(), careProvider.getOwningKey()))
                    .addReferenceState(new ReferencedStateAndRef<>(Objects.requireNonNull(bnService.getMembership(networkId, getOurIdentity()))))
                    .addReferenceState(new ReferencedStateAndRef<>(Objects.requireNonNull(bnService.getMembership(networkId, careProvider))
                    ));
            txBuilder.verify(getServiceHub());

            SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession session = initiateFlow(careProvider);
            SignedTransaction ftx = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(session)));
            return subFlow(new FinalityFlow(ftx, Arrays.asList(session)));
        }


        /**
         * Verifies that [lender] and [borrower] are members of Business Network with [networkId] ID, their memberships are active, contain
         * business identity of [BankIdentity] type and that lender is authorised to issue the loan.
         */
        @Suspendable
        protected void businessNetworkFullVerification(String networkId, Party policyIssuer, Party careProvider) throws MembershipNotFoundException {
            BNService bnService = getServiceHub().cordaService(BNService.class);
            try{
                MembershipState policyIssuerMembership = bnService.getMembership(networkId,policyIssuer).getState().getData();
                if (!policyIssuerMembership.isActive()){
                    throw new IllegalMembershipStatusException("$policyIssuer is not active member of Business Network with $networkId ID");
                }
                if(policyIssuerMembership.getIdentity().getBusinessIdentity().getClass()!= InsurerIdentity.class){
                    throw new IllegalMembershipBusinessIdentityException("$policyIssuer business identity should be InsurerIdentity");
                }
                Set<BNRole> setRoles = policyIssuerMembership.getRoles();
                for (BNRole role : setRoles){
                    if(!role.getPermissions().contains(InsurerIdentity.IssuePermissions.CAN_ISSUE_POLICY)){
                        throw new MembershipAuthorisationException("$policyIssuer is not authorised to issue insurance Polict in Business Network with $networkId ID");
                    }
                }
            } catch (Exception e){
                throw new MembershipNotFoundException("$policyIssuer is not member of Business Network with $networkId ID");
            }
            try{
                MembershipState careProviderMembership = bnService.getMembership(networkId,careProvider).getState().getData();
                if (!careProviderMembership.isActive()){
                    throw new IllegalMembershipStatusException("$policyIssuer is not active member of Business Network with $networkId ID");
                }
                if(careProviderMembership.getIdentity().getBusinessIdentity().getClass()!= CareProviderIdentity.class){
                    throw new IllegalMembershipBusinessIdentityException("$policyIssuer business identity should be InsurerIdentity");
                }
            } catch (Exception e){
                throw new MembershipNotFoundException("$policyIssuer is not member of Business Network with $networkId ID");
            }
        }

        @Suspendable
        private Memberships businessNetworkPartialVerification(String networkId, Party insurer, Party careProvider) throws MembershipNotFoundException {
            BNService bnService = getServiceHub().cordaService(BNService.class);
            StateAndRef<MembershipState> insurerMembership = null;
            try {
                insurerMembership = bnService.getMembership(networkId,insurer);
            }catch(Exception e){
                throw new MembershipNotFoundException("insurer is not part of Business Network with $networkId ID");
            }
            StateAndRef<MembershipState> careProMembership = null;
            try {
                careProMembership = bnService.getMembership(networkId,careProvider);
            }catch(Exception e){
                throw new MembershipNotFoundException("careProvider is not part of Business Network with $networkId ID");
            }

            return new Memberships(insurerMembership,careProMembership);
        }

    }

    @InitiatedBy(IssuePolicyInitiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartySession;

        public Acceptor(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    Command command = stx.getTx().getCommands().get(0);
                    if (!(command.getValue() instanceof InsuranceStateContract.Commands.Issue)){
                        throw new FlowException("Only LoanContract.Commands.Issue command is allowed");
                    }

                    InsuranceState insuranceState = (InsuranceState) stx.getTx().getOutputStates().get(0);
                    if (!(insuranceState.getInsurer().equals(otherPartySession.getCounterparty()))){
                        throw new FlowException("insurer doesn't match sender's identity");
                    }
                    if(!(insuranceState.getCareProvider().equals(getOurIdentity()))){
                        throw new FlowException("careProvider doesn't match receiver's identity");
                    }
                }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId = subFlow(signTxFlow).getId();
            return subFlow(new ReceiveFinalityFlow(otherPartySession, txId));
        }
    }

    static class Memberships{
        private StateAndRef<MembershipState> MembershipA;
        private StateAndRef<MembershipState> MembershipB;

        public Memberships(StateAndRef<MembershipState> membershipA, StateAndRef<MembershipState> membershipB) {
            MembershipA = membershipA;
            MembershipB = membershipB;
        }

        public StateAndRef<MembershipState> getMembershipA() {
            return MembershipA;
        }

        public StateAndRef<MembershipState> getMembershipB() {
            return MembershipB;
        }
    }

    static class IllegalMembershipBusinessIdentityException extends FlowException{
        public IllegalMembershipBusinessIdentityException(@Nullable String message) {
            super(message);
        }
    }

}
