package net.corda.samples.tokentofriend.states;

import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import net.corda.samples.tokentofriend.contracts.CustomTokenContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@BelongsToContract(CustomTokenContract.class)
public class CustomTokenState extends EvolvableTokenType {

    private String issuer;
    private String receipient;
    private String message;
    private Party maintainer;
    private int fractionDigits;
    private UniqueIdentifier linearId;

    public CustomTokenState(String issuer, String receipient, String message, Party maintainer, int fractionDigits, UniqueIdentifier linearId) {
        this.issuer = issuer;
        this.receipient = receipient;
        this.message = message;
        this.maintainer = maintainer;
        this.fractionDigits = fractionDigits;
        this.linearId = linearId;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getReceipient() {
        return receipient;
    }

    public String getMessage() {
        return message;
    }

    public Party getMaintainer() {
        return maintainer;
    }

    @Override
    public int getFractionDigits() {
        return fractionDigits;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setReceipient(String receipient) {
        this.receipient = receipient;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMaintainer(Party maintainer) {
        this.maintainer = maintainer;
    }

    public void setFractionDigits(int fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    public void setLinearId(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        List<Party> maintainers = new ArrayList<Party>();
        maintainers.add(this.maintainer);
        return maintainers;
    }

    /* This method returns a TokenPointer by using the linear Id of the evolvable state */
    public TokenPointer<CustomTokenState> toPointer(){
        LinearPointer<CustomTokenState> linearPointer = new LinearPointer<>(linearId, CustomTokenState.class);
        return new TokenPointer<>(linearPointer, fractionDigits);
    }
}
