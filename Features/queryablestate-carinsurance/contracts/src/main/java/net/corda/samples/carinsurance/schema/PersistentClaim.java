package net.corda.samples.carinsurance.schema;

import jakarta.persistence.*;
import java.util.UUID;
//4.6 changes
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


/**
 * JPA Entity for saving claim details to the database table
 */
@Entity
@Table(name = "CLAIM_DETAIL")
public class PersistentClaim {

    @Id @JdbcTypeCode(SqlTypes.VARCHAR) private final UUID id;
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
