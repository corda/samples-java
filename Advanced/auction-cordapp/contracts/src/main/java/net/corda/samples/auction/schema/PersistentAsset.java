package net.corda.samples.auction.schema;

import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "ASSET_DETAIL")
public class PersistentAsset extends PersistentState implements Serializable {

    @Column private final String assetId;
    @Column private final String title;
    @Column
    @Lob
    private final String description;
    @Column
    @Lob
    private final String imageURL;
    @Column private final String owner;

    public PersistentAsset() {
        this.assetId = null;
        this.title = null;
        this.description = null;
        this.imageURL = null;
        this.owner = null;
    }

    public PersistentAsset(String assetId, String title, String description, String imageURL, String owner) {
        this.assetId = assetId;
        this.title = title;
        this.description = description;
        this.imageURL = imageURL;
        this.owner = owner;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getOwner() {
        return owner;
    }
}
