package net.corda.samples.dollartohousetoken.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Currency;
import java.util.List;

/*
 * Responder Flow for the sale of house in exchange of fiat-currency. This flow receives the valuation of the house from
 * the seller and transfer the equivalent amount of fiat currency to the seller.
 */
@InitiatedBy(HouseSaleInitiatorFlow.class)
public class HouseSaleResponderFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession counterpartySession;

    public HouseSaleResponderFlow(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        /* Receive the valuation of the house */
        Amount<Currency> price =  counterpartySession.receive(Amount.class).unwrap(amount -> amount);

        /* Create instance of the fiat currency token amount */
        Amount<TokenType> priceToken = new Amount<>(price.getQuantity(), FiatCurrency.Companion.getInstance(price.getToken().getCurrencyCode()));


        /* Generate the move proposal, it returns the input-output pair for the fiat currency transfer, which we need to
        send to the Initiator */
        PartyAndAmount<TokenType> partyAndAmount = new PartyAndAmount<>(counterpartySession.getCounterparty(), priceToken);
        Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs = new DatabaseTokenSelection(getServiceHub())
                // here we are generating input and output states which send the correct amount to the seller, and any change back to buyer
                .generateMove(Collections.singletonList(new Pair<>(counterpartySession.getCounterparty(), priceToken)), getOurIdentity());

        /* Call SendStateAndRefFlow to send the inputs to the Initiator */
        subFlow(new SendStateAndRefFlow(counterpartySession, inputsAndOutputs.getFirst()));

        /* Send the output generated from the fiat currency move proposal to the initiator */
        counterpartySession.send(inputsAndOutputs.getSecond());
        subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });
        return subFlow(new ReceiveFinalityFlow(counterpartySession));
    }
}
