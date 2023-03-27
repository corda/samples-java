package net.corda.samples.snl.service

import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken


@CordaService
class GameSyncService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken()
