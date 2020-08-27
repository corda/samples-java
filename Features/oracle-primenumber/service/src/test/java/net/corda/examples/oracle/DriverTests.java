package net.corda.examples.oracle;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.examples.oracle.base.contract.PrimeState;
import net.corda.examples.oracle.client.flow.CreatePrime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DriverTests {
    private Logger logger;
    private CordaRPCConnection oracleCon;
    private CordaRPCConnection partyACon;
    private CordaRPCConnection partyBCon;

    private CordaRPCOps oracleRPCOps;
    private CordaRPCOps partyARPCOps;
    private CordaRPCOps partyBRPCOps;

    private CountDownLatch lock = new CountDownLatch(1);

    @Before
    public void setUp() {
        setConnections();
    }

    @After
    public void tearDown() {
        oracleCon.close();
        partyACon.close();
        partyBCon.close();
    }

    @Test
    public void getPrimeTest() throws InterruptedException {
        CompletableFuture.allOf(
                partyARPCOps.startFlowDynamic(CreatePrime.class, 100).getReturnValue().toCompletableFuture(),
                partyBRPCOps.startFlowDynamic(CreatePrime.class, 200).getReturnValue().toCompletableFuture()
        ).thenRun(
                () -> {
                    System.out.println(partyARPCOps.vaultQuery(PrimeState.class).getStates());
                    System.out.println(partyBRPCOps.vaultQuery(PrimeState.class).getStates());
                    lock.countDown();
                }
        );
        lock.await(2000, TimeUnit.MILLISECONDS);
        /*
         optional .thenRun chaining but for real-time you may need to do some sort of vault-track to console or embed
         outputs in the flow itself which will then show in the driver terminal.
        */
    }

    private void setConnections() {
        // For more information on setting up RPC client connections goto
        // https://docs.corda.net/docs/corda-os/4.5/clientrpc.html

        // username and password - these are default
        final String username = "user1";
        final String password = "test";

        // setup network host and port
        final NetworkHostAndPort oracleHP = NetworkHostAndPort.parse("localhost:10035");
        final NetworkHostAndPort partyAHP = NetworkHostAndPort.parse("localhost:10015");
        final NetworkHostAndPort partyBHP = NetworkHostAndPort.parse("localhost:10025");

        // Create connections for different nodes : note you can chain but best to keep a
        // reference to CordaRPCConnection so you can later close it.
        oracleCon = new CordaRPCClient(oracleHP).start(username, password);
        partyACon = new CordaRPCClient(partyAHP).start(username, password);
        partyBCon = new CordaRPCClient(partyBHP).start(username, password);

        // Retrieve proxys for node interaction
        oracleRPCOps = oracleCon.getProxy();
        partyARPCOps = partyACon.getProxy();
        partyBRPCOps = partyBCon.getProxy();
    }

}
