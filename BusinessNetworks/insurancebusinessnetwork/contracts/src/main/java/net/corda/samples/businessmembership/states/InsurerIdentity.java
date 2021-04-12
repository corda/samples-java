package net.corda.samples.businessmembership.states;

import net.corda.bn.states.BNIdentity;
import net.corda.bn.states.BNPermission;
import net.corda.bn.states.BNRole;
import net.corda.core.serialization.CordaSerializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

/**
 * Custom Identity #1
 * Business identity specific for Insurer Companies. Uses mimicking Swift Business Identifier Code (BIC).
 *
 * @property bic Business Identifier Code of the bank.
 */
@CordaSerializable
public class InsurerIdentity implements BNIdentity {

    private String insuranceIdentityCode;
    private String iicRegex = "^[a-zA-Z]{6}[0-9a-zA-Z]{2}([0-9a-zA-Z]{3})?$";


    public InsurerIdentity(String insuranceIdentityCode) {
        this.insuranceIdentityCode = insuranceIdentityCode;
    }

    public String getInsuranceIdentityCode() {
        return insuranceIdentityCode;
    }

    public String getIicRegex() {
        return iicRegex;
    }

    public boolean isValid(){
        return this.insuranceIdentityCode.matches(iicRegex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InsurerIdentity)) return false;
        InsurerIdentity that = (InsurerIdentity) o;
        return Objects.equals(getInsuranceIdentityCode(), that.getInsuranceIdentityCode()) && Objects.equals(getIicRegex(), that.getIicRegex());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInsuranceIdentityCode(), getIicRegex());
    }

    @CordaSerializable
    public static class PolicyIssuerRole extends BNRole {
        public PolicyIssuerRole() {
            super("PolicyIssuer", new HashSet<BNPermission>(Collections.singleton(IssuePermissions.CAN_ISSUE_POLICY)));
        }
    }
    @CordaSerializable
    public enum IssuePermissions implements BNPermission {
        /** Enables Business Network member to issue [InsuranceClaim]s. **/
        CAN_ISSUE_POLICY
    }
}
