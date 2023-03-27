package net.corda.samples.referencestates.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.samples.referencestates.contracts.SanctionedEntitiesContract
import net.corda.samples.referencestates.states.SanctionedEntities
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

object UpdateSanctionsListFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val partyToSanction: Party) : FlowLogic<StateAndRef<SanctionedEntities>>() {
        val ADDING_PARTY_TO_LIST = Step("Sanctioning Party: ${partyToSanction.name}")
        val GENERATING_TRANSACTION = Step("Generating Transaction")
        val SIGNING_TRANSACTION = Step("Signing transaction with our private key.")

        object FINALISING_TRANSACTION : Step("Recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
            GENERATING_TRANSACTION,
            ADDING_PARTY_TO_LIST,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION
        )

        override val progressTracker = tracker()

        /**
         * The flows logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): StateAndRef<SanctionedEntities> {
            // Obtain a reference from a notary we wish to use.
            val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2

            val oldList = serviceHub.vaultService.queryBy(SanctionedEntities::class.java).states.single()
            val newList = oldList.state.data.copy(badPeople = oldList.state.data.badPeople + listOf(partyToSanction))
            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val txCommand =
                Command(SanctionedEntitiesContract.Commands.Update, serviceHub.myInfo.legalIdentities.first().owningKey)
            val txBuilder = TransactionBuilder(notary)
                .addOutputState(newList, SanctionedEntitiesContract.SANCTIONS_CONTRACT_ID)
                .addInputState(oldList)
                .addCommand(txCommand)
            progressTracker.currentStep = ADDING_PARTY_TO_LIST

            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(
                FinalityFlow(
                    partSignedTx,
                    sessions = emptyList(),
                    progressTracker = FINALISING_TRANSACTION.childProgressTracker()
                )
            ).tx.outRefsOfType(SanctionedEntities::class.java).single()
        }
    }
}
