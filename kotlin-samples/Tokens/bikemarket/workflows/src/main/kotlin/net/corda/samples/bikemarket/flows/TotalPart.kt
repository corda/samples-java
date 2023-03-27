package net.corda.samples.bikemarket.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokensHandler
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemNonFungibleTokensHandler
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.bikemarket.states.FrameTokenState
import net.corda.samples.bikemarket.states.WheelsTokenState

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class TotalPart(val part: String,
                   val serial: String) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        if (part.equals("frame")){
            val frameSerial = serial
            //transfer frame token
            val frameStateAndRef = serviceHub.vaultService.queryBy<FrameTokenState>().states
                    .filter { it.state.data.serialNum.equals(frameSerial) }[0]

            //get the TokenType object
            val frametokentype = frameStateAndRef.state.data
            val issuer = frametokentype.maintainer

            //get the pointer pointer to the frame
            val frametokenPointer: TokenPointer<*> = frametokentype.toPointer(frametokentype.javaClass)

            val stx = subFlow(RedeemNonFungibleTokens(frametokenPointer,issuer))
            return "\nThe frame part is totaled, and the token is redeem to BikeCo" + "\nTransaction ID: " + stx.id

        }else if(part.equals("wheels")){
            val wheelsSerial = serial
            //transfer wheel token
            val wheelStateAndRef = serviceHub.vaultService.queryBy<WheelsTokenState>().states
                    .filter { it.state.data.serialNum.equals(wheelsSerial) }[0]

            //get the TokenType object
            val wheeltokentype: WheelsTokenState = wheelStateAndRef.state.data
            val issuer = wheeltokentype.maintainer

            //get the pointer pointer to the wheel
            val wheeltokenPointer: TokenPointer<*> = wheeltokentype.toPointer(wheeltokentype.javaClass)
            val stx = subFlow(RedeemNonFungibleTokens(wheeltokenPointer, issuer))
            return "\nThe wheels part is totaled, and the token is redeem to BikeCo" + "\nTransaction ID: " + stx.id

        }else{
            return "Please enter either frame or wheels for parameter part."
        }
    }
}

@InitiatedBy(TotalPart::class)
class TotalPartResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
        subFlow(RedeemNonFungibleTokensHandler(counterpartySession));
    }
}

