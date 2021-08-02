package net.corda.samples.chainmail;

import net.corda.client.rpc.RPCConnection;
import net.corda.client.rpc.ext.MultiRPCClient;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static net.corda.core.utilities.NetworkHostAndPort.parse;

/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        // Create an RPC connection to the node.
        if (args.length != 3) throw new IllegalArgumentException("Usage: Client <node address> <rpc username> <rpc password>");
        final NetworkHostAndPort nodeAddress = parse(args[0]);
        final String rpcUsername = args[1];
        final String rpcPassword = args[2];
//        final CordaRPCClient client = new CordaRPCClient(nodeAddress);
//        final CordaRPCConnection clientConnection = client.start(rpcUsername, rpcPassword);
//        final CordaRPCOps proxy = clientConnection.getProxy();

        System.out.println("ATTEMPTING MULTIRPC CLIENT FROM CLIENT SIDE");
        final MultiRPCClient client = new MultiRPCClient(nodeAddress, CordaRPCOps.class, rpcUsername, rpcPassword);
        final CompletableFuture<RPCConnection<CordaRPCOps>> clientConnection = client.start();
        final CordaRPCOps proxy;
        final List<NodeInfo> nodes;
        try {
            proxy = clientConnection.get().getProxy();
            nodes = proxy.networkMapSnapshot();

            // Interact with the node.
            // Example #1, here we print the nodes on the network.
            System.out.println("\n-- Here is the networkMap snapshot --");
            logger.info("{}", nodes);

            // Example #2, here we print the PartyA's node info
            CordaX500Name name = proxy.nodeInfo().getLegalIdentities().get(0).getName();//nodeInfo().legalIdentities.first().name
            System.out.println("\n-- Here is the node info of the node that the client connected to --");
            logger.info("{}", name);

            //Close the client connection
            clientConnection.get().close();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }
}
