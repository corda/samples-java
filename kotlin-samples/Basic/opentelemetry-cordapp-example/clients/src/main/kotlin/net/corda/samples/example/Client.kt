package net.corda.samples.example

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.context.Scope
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.loggerFor
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.samples.example.flows.ExampleFlow
import java.lang.IllegalStateException
import java.util.*


/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = Client().main(args)

private class Client {
    companion object {
        val logger = loggerFor<Client>()
    }

    fun main(args: Array<String>) {
        // Create an RPC connection to the node.
        require(args.size == 3) { "Usage: Client <node address> <rpc username> <rpc password>" }
        val nodeAddress = parse(args[0])
        val rpcUsername = args[1]
        val rpcPassword = args[2]
        val client = CordaRPCClient(nodeAddress)
        val clientConnection = client.start(rpcUsername, rpcPassword)

        // Get an open telemetry handle from the client connection. Once you have the
        // handle you can use the open telemetry api to create spans.
        val telemetry = clientConnection.getTelemetryHandle(OpenTelemetry::class.java)
        val tracer = telemetry?.getTracer("ClientRPC")
        val proxy = clientConnection.proxy

        val span = tracer?.spanBuilder("my span in client")?.startSpan()
        try {
            span?.makeCurrent().use {
                val myBaggage: Map<String, String>? = mapOf("baggage.from.my.client" to "baggage from my client - ${UUID.randomUUID()}")
                val baggage = myBaggage?.toList()?.fold(Baggage.current().toBuilder()) { builder, attribute ->
                    builder.put(
                        attribute.first,
                        attribute.second
                    )
                }
                baggage?.build()?.makeCurrent().use {
                    // Interact with the node.
                    // Example #1, here we print the nodes on the network.
                    val nodes = proxy.networkMapSnapshot()
                    println("\n-- Here is the networkMap snapshot --")
                    logger.info("{}", nodes)

                    // Example #2, here we print the PartyA's node info
                    val me = proxy.nodeInfo().legalIdentities.first().name
                    println("\n-- Here is the node info of the node that the client connected to --")
                    logger.info("{}", me)

                    val iouValue = 50
                    val otherParty: Party = proxy.wellKnownPartyFromX500Name(CordaX500Name("PartyB", "New York", "US"))
                        ?: throw IllegalStateException("Count not find Party")
                    val signedTx =
                        proxy.startFlow(ExampleFlow::Initiator, iouValue, otherParty).returnValue.getOrThrow()
                    println("\n-- Signed transaction id from flow: ${signedTx.tx.id}")
                }
            }
        }
        finally {
            span?.end()
        }

        // Close the client connection.
        // Note that this also shuts down telemetry so must be done last after all spans have ended.
        clientConnection.close()
    }
}