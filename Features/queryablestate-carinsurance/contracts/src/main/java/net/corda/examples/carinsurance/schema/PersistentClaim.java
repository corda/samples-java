package net.corda.examples.carinsurance.schema;

import javax.persistence.*;
import java.util.UUID;
//4.6 changes
import org.hibernate.annotations.Type;


/**
 * JPA Entity for saving claim details to the database table
 */
@Entity
@Table(name = "CLAIM_DETAIL")
public class PersistentClaim {

    @Id @Type (type = "uuid-char") private final UUID id;
    @Column private final String claimNumber;
    @Column private final String claimDescription;
    @Column private final Integer claimAmount;

    /**
     * Default constructor required by Hibernate
     */
    public PersistentClaim() {
        this.id = null;
        this.claimNumber = null;
        this.claimDescription = null;
        this.claimAmount = null;
    }

    public PersistentClaim(String claimNumber, String claimDescription, Integer claimAmount) {
        this.id = UUID.randomUUID();
        this.claimNumber = claimNumber;
        this.claimDescription = claimDescription;
        this.claimAmount = claimAmount;
    }

    public UUID getId() {
        return id;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public String getClaimDescription() {
        return claimDescription;
    }

    public Integer getClaimAmount() {
        return claimAmount;
    }
}
