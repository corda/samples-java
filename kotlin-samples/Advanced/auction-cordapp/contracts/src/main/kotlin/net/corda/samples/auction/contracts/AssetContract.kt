package net.corda.samples.auction.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.auction.states.Asset
import net.corda.samples.auction.states.AuctionState

// ************
// * Contract *
// ************
class AssetContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "net.corda.samples.auction.contracts.AssetContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        if(tx.commands.isEmpty()){
            throw IllegalArgumentException("One command Expected")
        }
        val command = tx.commands.get(0)
        when (command.value){
            is AssetContract.Commands.CreateAsset -> requireThat {
                val asset = tx.outputStates.single() as Asset
                "Asset Must Have Description" using (asset.description != "")
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class TransferAsset : Commands
        class CreateAsset: Commands
    }
}