package net.corda.samples.snl.service;

import net.corda.core.crypto.TransactionSignature;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


@CordaService
public class DiceRollService extends SingletonSerializeAsToken {

    private final AppServiceHub serviceHub;
    private Map<String, Integer> rollMap;

    public DiceRollService(AppServiceHub serviceHub) {
        this.serviceHub = serviceHub;
        this.rollMap = new HashMap<>();
    }

    public Integer diceRoll (String player){
        Random ran = new Random();
        int roll =  ran.nextInt(6) + 1;
        rollMap.put(player, roll);
        return roll;
    }

    public TransactionSignature sign(FilteredTransaction transaction) throws FilteredTransactionVerificationException {

        transaction.verify();

        boolean isValid = true; // Additional verification logic here.

        if(isValid){
            return serviceHub.createSignature(transaction, serviceHub.getMyInfo().getLegalIdentities().get(0).getOwningKey());
        }else{
            throw new IllegalArgumentException();
        }
    }
}
