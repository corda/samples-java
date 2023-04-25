package net.corda.samples.auction.schema;

import net.corda.core.schemas.MappedSchema;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class AssetSchemaV1 extends MappedSchema {

    public AssetSchemaV1() {
        super(AssetSchemaFamily.class, 1, Collections.singleton(PersistentAsset.class));
    }

    @Nullable
    @Override
    public String getMigrationResource() {
        return "asset.changelog-master";
    }
}
