package net.corda.samples.contractsdk.states

import net.corda.core.serialization.CordaSerializable


// define some types of record player needles
@CordaSerializable
enum class Needle {
    SPHERICAL, ELLIPTICAL, DAMAGED
}
