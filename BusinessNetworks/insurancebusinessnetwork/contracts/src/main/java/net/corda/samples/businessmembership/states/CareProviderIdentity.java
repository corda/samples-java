package net.corda.samples.businessmembership.states;

import net.corda.bn.states.BNIdentity;
import net.corda.bn.states.BNPermission;
import net.corda.bn.states.BNRole;
import net.corda.core.serialization.CordaSerializable;

import java.util.Collections;
import java.util.HashSet;
/**
 * Custom Identity #2
 * Business identity specific for CareProvider Companies. Uses mimicking Swift Business Identifier Code (BIC).
 *
 * @property bic Business Identifier Code of the bank.
 */
@CordaSerializable
public class CareProviderIdentity implements BNIdentity {

    private String CareProviderIdentityCode;
    private String cicRegex = "^[a-zA-Z]{6}[0-9a-zA-Z]{2}([0-9a-zA-Z]{3})?$";

    public CareProviderIdentity(String careProviderIdentityCode) {
        CareProviderIdentityCode = careProviderIdentityCode;
    }

    public String getCareProviderIdentityCode() {
        return CareProviderIdentityCode;
    }

    public String getCicRegex() {
        return cicRegex;
    }

    public boolean isValid(){
        return this.CareProviderIdentityCode.matches(cicRegex);
    }

    @CordaSerializable
    public static class PsychiatryRole extends BNRole {
        public PsychiatryRole() {
            super("Psychiatrists", new HashSet<BNPermission>(
                    Collections.singleton(PsychiatryPermissions.CAN_TREAT_MENTAL_HEALTH_ISSUE)));
        }
    }
    @CordaSerializable
    public enum PsychiatryPermissions implements BNPermission {
        /** Enables Business Network member to issue [InsuranceClaim]s. **/
        CAN_TREAT_MENTAL_HEALTH_ISSUE
    }

    @CordaSerializable
    public static class OrthodonticsRole extends BNRole {
        public OrthodonticsRole() {
            super("Orthodontics", new HashSet<BNPermission>(
                    Collections.singleton(OrthodonticsPermissions.CAN_TREAT_ORAL_ISSUE)));
        }
    }
    @CordaSerializable
    public enum OrthodonticsPermissions implements BNPermission {
        /** Enables Business Network member to issue [InsuranceClaim]s. **/
        CAN_TREAT_ORAL_ISSUE
    }

}
