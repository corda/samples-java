package net.corda.samples.snl.service;

import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;

@CordaService
public class GameSyncService extends SingletonSerializeAsToken {

    private AppServiceHub serviceHub;

    public GameSyncService(AppServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }


}
