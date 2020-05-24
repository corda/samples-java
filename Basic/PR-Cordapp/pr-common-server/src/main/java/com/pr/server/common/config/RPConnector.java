package com.pr.server.common.config;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

@Component
public class RPConnector {
    private static final Logger logger = LoggerFactory.getLogger(RPConnector.class);
    @Value("${config.rpc.address}")
    private String rpcAddress;
    @Value("${config.rpc.username}")
    private String username;
    @Value("${config.rpc.password}")
    private String password;
    @Value("${nodename}")
    private String nodename;

    private CordaRPCOps rpcOps;

    @PostConstruct
    private void init() {
        logger.info(String.format("RPConnector [host=%s, username=%s, nodename=%s]", rpcAddress, username, nodename));
        NetworkHostAndPort hostAndPort = NetworkHostAndPort.parse(rpcAddress);
        CordaRPCClient rpcClient = new CordaRPCClient(hostAndPort);
        CordaRPCConnection rpcConnection = rpcClient.start(username, password);
        rpcOps = rpcConnection.getProxy();
        logger.info(String.format("connected to via RPC to %s", nodename));
        rpcOps.networkMapSnapshot();
    }

    public CordaRPCOps getRPCops() {
        return rpcOps;
    }

    public String getRpcAddress() {
        return rpcAddress;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNodeName() {
        return nodename;
    }
}
