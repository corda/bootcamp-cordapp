package com.bootcamp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.money.DigitalCurrency;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.money.MoneyUtilities;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import com.r3.corda.lib.tokens.contracts.types.TokenType;

import java.util.Collections;

@StartableByRPC
public class CreateAndIssueFixedToken extends FlowLogic<SignedTransaction> {

    private String currencyCode;
    private Long amount;
    private Party recipient;

    public CreateAndIssueFixedToken(String currencyCode, Long amount, Party recipient) {
        this.currencyCode = currencyCode;
        this.amount = amount;
        this.recipient = recipient;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        TokenType tokenType;

        switch (currencyCode) {
            case "USD":
                // MoneyUtilities returns either a TokenType or Amount<TokenType> related to standard currencies
                tokenType = MoneyUtilities.getUSD();
                break;
            case "AUD":
                tokenType = MoneyUtilities.getAUD();
                break;
            case "GBP":
                // FiatCurrency returns a TokenType from an ISO currency code
                tokenType = FiatCurrency.getInstance(currencyCode);
                break;
            case "BTC":
                // DigitalCurrency returns a TokenType related to standard crypto/digital currencies
                tokenType = DigitalCurrency.getInstance(currencyCode);
                break;
            default:
                throw new FlowException("unable to generate currency");
        }

        // The FungibleTokenBuilder allows quick and easy stepwise assembly of a token that can be split/merged
        FungibleToken tokens = new FungibleTokenBuilder()
                .ofTokenType(tokenType)
                .withAmount(amount)
                .issuedBy(getOurIdentity())
                .heldBy(recipient)
                .buildFungibleToken();

        return subFlow(new IssueTokens(Collections.singletonList(tokens)));
    }
}
