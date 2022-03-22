package com.bootcamp.flows;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
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

    private String currenctCode;
    private Long amount;
    private Party recipient;

    public CreateAndIssueFixedToken(String currenctCode, Long amount, Party recipient) {
        this.currenctCode = currenctCode;
        this.amount = amount;
        this.recipient = recipient;
    }

    @Override
    public SignedTransaction call() throws FlowException {

        TokenType tokenType = new TokenType(currenctCode,2);

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
