package net.corda.examples.oracle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.TestCordapp;
import net.corda.testing.node.User;

import java.util.List;

import static net.corda.testing.driver.Driver.driver;

/**
 * Allows you to run your nodes through an IDE (as opposed to using deployNodes). Do not use in a production
 * environment.
 */
public class NodeDriver {

    public static void main(String[] args) {
        final List<User> rpcUsers =
                ImmutableList.of(new User("user1", "test", ImmutableSet.of("ALL")));

        driver(new DriverParameters()
                        .withCordappsForAllNodes( ImmutableSet.of(
                                    TestCordapp.findCordapp("net.corda.examples.oracle.service.service"),
                                    TestCordapp.findCordapp("net.corda.examples.oracle.base.contract")
                                ))
                        .withStartNodesInProcess(true)
                        .withWaitForAllNodesToFinish(true), dsl -> {
                    try {
                        NodeHandle oracle = dsl.startNode(new NodeParameters()
                                .withCustomOverrides(ImmutableMap.of("rpcSettings.address", "localhost:10035"))
                                .withProvidedName(new CordaX500Name("Oracle", "New York","US"))
                                .withRpcUsers(rpcUsers)).get();
                        NodeHandle nodeA = dsl.startNode(new NodeParameters()
                                .withCustomOverrides(ImmutableMap.of("rpcSettings.address", "localhost:10015"))
                                .withProvidedName(new CordaX500Name("PartyA", "London", "GB"))
                                .withRpcUsers(rpcUsers)).get();
                        NodeHandle nodeB = dsl.startNode(new NodeParameters()
                                .withCustomOverrides(ImmutableMap.of("rpcSettings.address", "localhost:10025"))
                                .withProvidedName(new CordaX500Name("PartyB", "New York", "US"))
                                .withRpcUsers(rpcUsers)).get();

                    } catch (Throwable e) {
                        System.err.println("Encountered exception in node startup: " + e.getMessage());
                        e.printStackTrace();
                    }

                    return null;
                }

        );
    }
}
