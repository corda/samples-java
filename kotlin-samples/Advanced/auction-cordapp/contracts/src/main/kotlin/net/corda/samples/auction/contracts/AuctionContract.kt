package net.corda.samples.auction.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.auction.states.Asset
import net.corda.samples.auction.states.AuctionState

// ************
// * Contract *
// ************
class AuctionContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "net.corda.samples.auction.contracts.AuctionContract"
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
            is Commands.Bid -> requireThat {
                "One Input Expected" using (tx.inputStates.size == 1)
                "One Output Expected" using (tx.outputStates.size == 1)
                val input = tx.inputsOfType<AuctionState>().get(0)
                "Auction has Ended" using (input.active)
                val output = tx.outputsOfType<AuctionState>().get(0)
                "Bid Price should be greater than base price" using (output.highestBid!!.quantity >= input.basePrice.quantity)
                if(input.highestBid != null){
                    "Bid Price should be greater than previous highest bid" using (output.highestBid.quantity >= input.highestBid.quantity)
                }
            }
            is Commands.EndAuction -> requireThat {
                "One Output Expected" using (tx.outputStates.size == 1)
                val output = tx.outputsOfType<AuctionState>().get(0)
                val commandEnd = tx.commandsOfType<Commands.EndAuction>().get(0)
                "Auctioneer Signature Required" using (commandEnd.signers.contains(output.auctioneer!!.owningKey))
            }
            is Commands.Settlement -> requireThat {
                val input = tx.inputsOfType<AuctionState>().get(0)
                val commandSett = tx.commandsOfType<Commands.Settlement>().get(0)
                "Auction is Active" using (!input.active)
                "Auctioneer and Winner must Sign" using (commandSett.signers.contains(input.auctioneer!!.owningKey) &&
                        commandSett.signers.contains(input.winner?.owningKey))
            }
            is Commands.Exit -> requireThat {
                val input = tx.inputsOfType<AuctionState>().get(0)
                val commandExit = tx.commandsOfType<Commands.Exit>().get(0)
                val asset = tx.referenceInputRefsOfType<Asset>().get(0).state.data
                "Auction is Active" using (!input.active)
                if(input.winner != null){
                    "Auctioneer and Winner must Sign" using (commandExit.signers.contains(input.auctioneer!!.owningKey))
                            //&& commandExit.signers.contains(input.winner.owningKey))
                }
                "Auction not settled yet" using (asset.owner.owningKey.equals(input.winner!!.owningKey))
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class CreateAuction : Commands
        class Bid : Commands
        class EndAuction : Commands
        class Settlement : Commands
        class Exit : Commands
    }
}