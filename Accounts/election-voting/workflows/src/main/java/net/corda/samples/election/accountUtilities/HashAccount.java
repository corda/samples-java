package net.corda.samples.election.accountUtilities;

import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.flows.StartableByService;

import java.math.BigInteger;
import java.security.MessageDigest;

@StartableByRPC
@StartableByService
public class HashAccount extends FlowLogic<String> {
    private final String acctName;

    public HashAccount(String acctName) {
        this.acctName = acctName;
    }
    public String call() throws FlowException {

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] acctHash = md.digest(acctName.getBytes());
        // returns the first five characters of the hash for ease of understanding
        return new BigInteger(1, acctHash).toString().substring(0,5);
    }
}
