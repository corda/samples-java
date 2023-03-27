package net.corda.samples.dollartohousetoken.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.workflows.utilities.NonFungibleTokenBuilder;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.dollartohousetoken.states.HouseState;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.util.Currency;
import java.util.UUID;

/**
 * Flow to create and issue house tokens. Token SDK provides some in-built flows which could be called to Create and Issue tokens.
 * This flow should be called by the issuer of the token. The constructor takes the owner and other properties of the house as
 * input parameters, it first creates the house token onto the issuer's ledger and then issues it to the owner.
*/
@StartableByRPC
public class HouseTokenCreateAndIssueFlow extends FlowLogic<String> {

    private final Party owner;
    private final Amount<Currency> valuation;
    private final int noOfBedRooms;
    private final String constructionArea;
    private final String additionInfo;
    private final String address;

    public HouseTokenCreateAndIssueFlow(Party owner, Amount<Currency> valuation,
                                        int noOfBedRooms, String constructionArea,
                                        String additionInfo, String address) {
        this.owner = owner;
        this.valuation = valuation;
        this.noOfBedRooms = noOfBedRooms;
        this.constructionArea = constructionArea;
        this.additionInfo = additionInfo;
        this.address = address;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        // Obtain a reference to a notary we wish to use.
        /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

        /* Get a reference of own identity */
        Party issuer = getOurIdentity();

        /* Construct the output state */
        UniqueIdentifier uuid = UniqueIdentifier.Companion.fromString(UUID.randomUUID().toString());
        final HouseState houseState = new HouseState(uuid, ImmutableList.of(issuer),
                valuation, noOfBedRooms, constructionArea, additionInfo, address);

        /* Create an instance of TransactionState using the houseState token and the notary */
        TransactionState<HouseState> transactionState = new TransactionState<>(houseState, notary);

        /* Create the house token. Token SDK provides the CreateEvolvableTokens flow which could be called to create an
        evolvable token in the ledger.*/
        subFlow(new CreateEvolvableTokens(transactionState));

        /* Create an instance of the non-fungible house token with the owner as the token holder.
        * Notice the TokenPointer is used as the TokenType, since EvolvableTokenType is not TokenType, but is
        * a LinearState. This is done to separate the state info from the token so that the state can evolve independently.
        * */
        NonFungibleToken houseToken = new NonFungibleTokenBuilder()
                .ofTokenType(houseState.toPointer())
                .issuedBy(issuer)
                .heldBy(owner)
                .buildNonFungibleToken();

        /* Issue the house token by calling the IssueTokens flow provided with the TokenSDK */
        SignedTransaction stx = subFlow(new IssueTokens(ImmutableList.of(houseToken)));
        return "\nThe non-fungible house token is created with UUID: "+ uuid +". (This is what you will use in next step)"
                +"\nTransaction ID: "+stx.getId();

    }
}
