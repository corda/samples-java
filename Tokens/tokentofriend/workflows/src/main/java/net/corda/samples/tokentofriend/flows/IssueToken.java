package net.corda.samples.tokentofriend.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.NonFungibleTokenBuilder;
import net.corda.samples.tokentofriend.states.CustomTokenState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.*;
import java.util.stream.Collectors;

@StartableByRPC
public class IssueToken extends FlowLogic<String>{

    private String uuid;

    public IssueToken(String uuid) {
        this.uuid = uuid;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        /* Get a reference of own identity */
        Party issuer = getOurIdentity();

        /* Fetch the house state from the vault using the vault query */

        QueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                .withUuid(Arrays.asList(UUID.fromString(uuid))).withStatus(Vault.StateStatus.UNCONSUMED);
        CustomTokenState customTokenState = getServiceHub().getVaultService().queryBy(CustomTokenState.class,inputCriteria)
                .getStates().get(0).getState().getData();

        /*
         * Create an instance of IssuedTokenType, it is used by our Non-Fungible token which would be issued to the owner. Note that the IssuedTokenType takes
         * a TokenPointer as an input, since EvolvableTokenType is not TokenType, but is a LinearState. This is done to separate the state info from the token
         * so that the state can evolve independently.
         * IssuedTokenType is a wrapper around the TokenType and the issuer.
         * */
        IssuedTokenType issuedToken = new NonFungibleTokenBuilder()
                .issuedBy(getOurIdentity())
                .ofTokenType(customTokenState.toPointer(customTokenState.getClass()))
                .buildIssuedTokenType();

        /* Create an instance of the non-fungible house token with the owner as the token holder. The last paramter is a hash of the jar containing the TokenType, use the helper function to fetch it. */
        AbstractParty storageNode = storageSelector();
        UniqueIdentifier tokenId = new UniqueIdentifier();
        NonFungibleToken token = new NonFungibleToken(issuedToken,storageNode,tokenId);

        subFlow(new IssueTokens(Arrays.asList(token)));

        return "\nMessage: "+ customTokenState.getMessage() + "\nToken Id is: "+tokenId+"\nStorage Node is: "+storageNode;
    }

    public AbstractParty storageSelector(){
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        List<NodeInfo> allOtherNodes = getServiceHub().getNetworkMapCache().getAllNodes().stream().filter( it ->
                (!it.getLegalIdentities().get(0).equals(getOurIdentity())) && (!it.getLegalIdentities().get(0).equals(notary))
        ).collect(Collectors.toList());
        int pick = (int) (Math.random()*(allOtherNodes.size()*10)/10);
        return allOtherNodes.get(pick).getLegalIdentities().get(0);
    }

}
