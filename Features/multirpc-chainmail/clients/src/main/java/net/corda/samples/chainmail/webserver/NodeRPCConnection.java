package net.corda.samples.chainmail.webserver;

import net.corda.client.rpc.RPCConnection;
import net.corda.client.rpc.ext.MultiRPCClient;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Wraps an RPC connection to a Corda node.
 *
 * The RPC connection is configured using command line arguments.
 */
@Component
public class NodeRPCConnection implements AutoCloseable {
    // The host of the node we are connecting to.
    @Value("${config.rpc.host}")
    private String host;
    // The RPC port of the node we are connecting to.
    @Value("${config.rpc.username}")
    private String username;
    // The username for logging into the RPC client.
    @Value("${config.rpc.password}")
    private String password;
    // The password for logging into the RPC client.
    @Value("${config.rpc.port}")
    private int rpcPort;

//    private CordaRPCConnection rpcConnection;
//    private MultiRPCClient rpcConnection;
    private CompletableFuture<RPCConnection<CordaRPCOps>> rpcConnection;
    CordaRPCOps proxy;

    @PostConstruct
    public void initialiseNodeRPCConnection() throws ExecutionException, InterruptedException {
        NetworkHostAndPort rpcAddress = new NetworkHostAndPort(host, rpcPort);
        MultiRPCClient client = new MultiRPCClient(rpcAddress, CordaRPCOps.class, username, password);
        System.out.println("ATTEMPTING CLIENT START FROM NODERPCCONNECTION");
        System.out.println("RPCADDRESS: " + rpcAddress + host + rpcPort);
        rpcConnection = client.start();
//            try(RPCConnection<CordaRPCOps> conn = connFuture.get()) {
        RPCConnection<CordaRPCOps> conn = rpcConnection.get();
        System.out.println("TRIED RPCCONNECTION");
        System.out.println(conn.getProxy().nodeInfo());
//                assertNotNull(conn.getProxy().nodeInfo());
        proxy = conn.getProxy();


//        NetworkHostAndPort rpcAddress = new NetworkHostAndPort(host, rpcPort);
//        CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
//        rpcConnection = rpcClient.start(username, password);
//        proxy = rpcConnection.getProxy();
    }

    @PreDestroy
    public void close() {
        try {
            System.out.println("ATTEMPTING SERVER CLOSE");
            rpcConnection.get().notifyServerAndClose();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
