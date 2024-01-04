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
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/*
 * This flow transfers the equivalent amount of fiat currency to the seller.
 */
@StartableByRPC
public class FiatCurrencyMoveFlow extends FlowLogic<SignedTransaction> {

    private final String currency;
    private final Long amount;
    private final Party recipient;

    public FiatCurrencyMoveFlow(String currency, Long amount, Party recipient) {
        this.currency = currency;
        this.amount = amount;
        this.recipient = recipient;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        Optional<Long> optFoo = Optional.ofNullable(null);
        long longAmount = optFoo.orElse( this.amount );
        /* Create instance of the fiat currency token amount */
        Amount<TokenType> priceToken = new Amount<>(longAmount, FiatCurrency.Companion.getInstance(this.currency));

        /* Generate the move proposal, it returns the input-output pair for the fiat currency transfer, which we need to
        send to the Initiator */
        Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs = new DatabaseTokenSelection(getServiceHub())
                // here we are generating input and output states which send the correct amount to the seller, and any change back to buyer
                .generateMove(Collections.singletonList(new Pair<>(this.recipient, priceToken)), getOurIdentity());

        FlowSession counterpartySession = initiateFlow(this.recipient);
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
