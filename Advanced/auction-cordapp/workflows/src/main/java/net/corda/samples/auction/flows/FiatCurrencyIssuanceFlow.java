package net.corda.samples.auction.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.money.MoneyUtilities;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;

import java.util.List;
import java.util.stream.Collectors;


/**
 * This flows is used to build a transaction to issue an asset on the Corda Ledger, which can later be put on auction.
 * It creates a self issues transaction, the states is only issued on the ledger of the party who executes the flows.
 */
@InitiatingFlow
@StartableByRPC
public class FiatCurrencyIssuanceFlow extends FlowLogic<SignedTransaction> {

    private final int amount;
    private final String recipient;

    /**
     * Constructor to initialise flows parameters received from rpc.
     *
     * @param amount of the currency to be issued in ledger
     */
    public FiatCurrencyIssuanceFlow(int amount, String recipient) {
        this.amount = amount;
        this.recipient = recipient;
    }


    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        List<NodeInfo> matchedNodes = getServiceHub().getNetworkMapCache().getAllNodes().stream().filter(node -> node.getLegalIdentities().get(0).getName().getOrganisation().equals(recipient)).collect(Collectors.toList());

        if (matchedNodes.isEmpty()){
            throw new FlowException("No matching nodes are found with the name " + recipient);
        }

        Party recipientParty = matchedNodes.get(0).getLegalIdentities().get(0);

        // Create an instance of FungibleToken for the fiat currency to be issued
        FungibleToken fungibleToken = new FungibleTokenBuilder()
                .ofTokenType(MoneyUtilities.getUSD())
                .withAmount(amount)
                .issuedBy(getOurIdentity())
                .heldBy(recipientParty)
                .buildFungibleToken();

        return subFlow(new IssueTokens(ImmutableList.of(fungibleToken), ImmutableList.of(recipientParty)));
    }

}
