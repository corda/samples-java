package net.corda.samples.bikemarket.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.utilities.heldBy
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.bikemarket.states.FrameTokenState
import net.corda.samples.bikemarket.states.WheelsTokenState

// *********
// * Flows *
// *********
@StartableByRPC
class IssueNewBike(val frameSerial: String,
                   val wheelsSerial: String,
                   val holder: Party) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        //Step 1: Frame Token
        //get frame states on ledger
        val frameStateAndRef = serviceHub.vaultService.queryBy<FrameTokenState>().states
                .filter { it.state.data.serialNum.equals(frameSerial) }[0]

        //get the TokenType object
        val frametokentype = frameStateAndRef.state.data

        //get the pointer pointer to the frame
        val frametokenPointer = frametokentype.toPointer(frametokentype.javaClass)

        //assign the issuer to the frame type who will be issuing the tokens
        val frameissuedTokenType = frametokenPointer issuedBy ourIdentity

        //mention the current holder also
        val frametoken = frameissuedTokenType heldBy holder

        //Step 2: Wheels Token
        val wheelStateAndRef = serviceHub.vaultService.queryBy<WheelsTokenState>().states
                .filter { it.state.data.serialNum.equals(wheelsSerial) }[0]

        //get the TokenType object
        val wheeltokentype: WheelsTokenState = wheelStateAndRef.state.data

        //get the pointer pointer to the wheel
        val wheeltokenPointer: TokenPointer<*> = wheeltokentype.toPointer(wheeltokentype.javaClass)

        //assign the issuer to the wheel type who will be issuing the tokens
        val wheelissuedTokenType = wheeltokenPointer issuedBy ourIdentity

        //mention the current holder also
        val wheeltoken = wheelissuedTokenType heldBy holder

        //distribute the new bike (two token to be exact)
        //call built in flow to issue non fungible tokens
        val stx = subFlow(IssueTokens(listOf(frametoken,wheeltoken)))

        return ("\nA new bike is being issued to " + holder.name.organisation + " with frame serial: "
                + this.frameSerial + "; wheels serial: " + this.wheelsSerial + "\nTransaction ID: " + stx.id)
    }
}