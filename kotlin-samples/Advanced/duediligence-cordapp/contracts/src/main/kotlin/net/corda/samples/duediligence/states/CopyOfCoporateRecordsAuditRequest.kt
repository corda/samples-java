package net.corda.samples.duediligence.states

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.samples.duediligence.contracts.CorporateRecordsContract

@BelongsToContract(CorporateRecordsContract::class)
class CopyOfCoporateRecordsAuditRequest(val qualification :Boolean = false,
                                        val originalOwner: Party,
                                        val copyReceiver: Party,
                                        val originalRequestId: UniqueIdentifier,
                                        val originalReportTxId: SecureHash,
                                        val originalValidater: Party,
                                        override val participants: List<AbstractParty> = listOf(copyReceiver),
                                        override val linearId: UniqueIdentifier) : LinearState