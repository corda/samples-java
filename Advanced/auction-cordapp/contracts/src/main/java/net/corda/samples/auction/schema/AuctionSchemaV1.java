package net.corda.samples.auction.schema;

import net.corda.core.schemas.MappedSchema;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class AuctionSchemaV1 extends MappedSchema {

    public AuctionSchemaV1() {
        super(AuctionSchemaFamily.class, 1, Collections.singleton(PersistentAuction.class));
    }

    @Nullable
    @Override
    public String getMigrationResource() {
        return "auction.changelog-master";
    }
}
