package net.corda.samples.duediligence.contracts

import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.duediligence.CORPORATE_JAR_HASH
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest
import java.io.IOException
import java.util.ArrayList
import java.util.jar.JarInputStream


class CorporateRecordsContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "net.corda.samples.duediligence.contracts.CorporateRecordsContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands[0]

        //Propose request
        when (command.value){
            is Commands.Propose -> requireThat {
                "There are no inputs" using (tx.inputs.isEmpty())
                "Only one output state should be created." using (tx.outputs.size == 1)
                "The single output is of type CorporateRecords State" using (tx.outputsOfType(CorporateRecordsAuditRequest::class.java).size == 1)
            }
            is Commands.Validate -> requireThat {
                "Only one output state should be created." using(tx.outputs.size == 1)
                "The single output is of type CorporateRecords State" using(tx.outputsOfType(CorporateRecordsAuditRequest::class.java).size == 1)
            }
            is Commands.Reject -> requireThat {
                "Only one output state should be created." using(tx.outputs.size == 1)
                "The single output is of type CorporateRecords State" using(tx.outputsOfType(CorporateRecordsAuditRequest::class.java).size == 1)
            }
            is Commands.Share -> requireThat {
                // Constraints on the included blacklist.
                val nonContractAttachments = tx.attachments.filter { it !is ContractAttachment }
                "The transaction should have a single non-contract attachment" using (nonContractAttachments.size == 1)
                val attachment = nonContractAttachments.single()

                // In the future, Corda will support the signing of jars. We will then be able to restrict
                // the blacklist used to just those signed by party X.
                "The jar's hash should be correct" using (attachment.id == CORPORATE_JAR_HASH)

                // Extract the whitelisted company names from the JAR.

                // Extract the whitelisted company names from the JAR.
                val whitelistedCompanies: MutableList<String> = ArrayList()
                val attachmentJar: JarInputStream = attachment.openAsJAR()
                try {
                    while (attachmentJar.nextEntry.name != "whitelistedCorporateAuditors.txt") {
                        // Calling 'getNextEntry()' causes us to scroll through the JAR.
                    }
                    val bufferedReader = attachmentJar.bufferedReader()
                    var company = bufferedReader.readLine()
                    while (company != null) {
                        whitelistedCompanies.add(company)
                        company = bufferedReader.readLine()
                    }
                } catch (e: IOException) {
                    println("error reading whitelistedCorporateAuditors.txt")
                }

                // Constraints on the Whitelist parties
                val (state) = tx.referenceInputRefsOfType(CorporateRecordsAuditRequest::class.java)[0]
                val corporateRecords = state.data

                val participants: MutableList<Party> = ArrayList()
                val participantsOrgs: MutableList<String> = ArrayList()
                for (p in corporateRecords.participants) {
                    val participant = p as Party
                    participantsOrgs.add(participant.name.organisation)
                    participants.add(participant)
                }
                // overlap is whether any participants in the transaction belong to a whitelisted org.
                val overlap: MutableSet<String> = HashSet(whitelistedCompanies) //Crossland Savings & TCF National Bank Wisconsin
                overlap.retainAll(HashSet(participantsOrgs)) // intersection | TCF & PartyA
                "The agreement did not use any whitelisted auditors$overlap" using(!overlap.isEmpty())
                }
            }
        }
    }

    interface Commands : CommandData {
        class Propose : Commands
        class Validate : Commands
        class Reject : Commands
        class Share : Commands
        class Report : Commands
    }
