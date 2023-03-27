package net.corda.samples.carinsurance.states

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Claim(val claimNumber: String,
                 val claimDescription: String,
                 val claimAmount: Int)
