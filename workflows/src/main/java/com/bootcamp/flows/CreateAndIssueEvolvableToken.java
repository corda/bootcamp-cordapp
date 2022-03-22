package com.bootcamp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.bootcamp.states.StateTokenType;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.NonFungibleTokenBuilder;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;

import java.util.Arrays;
import java.util.UUID;


public class CreateAndIssueEvolvableToken {

    @StartableByRPC
    public static class CreateEvolvableToken extends FlowLogic<SignedTransaction> {

        private String importantInformationThatMayChange;

        public CreateEvolvableToken(String importantInformationThatMayChange) {
            this.importantInformationThatMayChange = importantInformationThatMayChange;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
            // We get a reference to our own identity.
            Party me = getOurIdentity();

            //create token type
            StateTokenType tokenType = new StateTokenType(this.importantInformationThatMayChange, me, new UniqueIdentifier(), 0);

            //wrap it with transaction state specifying the notary
            TransactionState transactionState = new TransactionState(tokenType, notary);

            //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
            return subFlow(new CreateEvolvableTokens(transactionState));
        }
    }


    @StartableByRPC
    public static class IssueEvolvableToken extends FlowLogic<SignedTransaction> {

        private final String tokenId;
        private final Party recipient;

        public IssueEvolvableToken(String tokenId, Party recipient) {
            this.tokenId = tokenId;
            this.recipient = recipient;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            QueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                    .withUuid(Arrays.asList(UUID.fromString(tokenId))).withStatus(Vault.StateStatus.UNCONSUMED);

            //get the TokenType object
            StateTokenType state = getServiceHub().getVaultService().queryBy(StateTokenType.class,inputCriteria)
                    .getStates().get(0).getState().getData();

            //mention the current holder also
            NonFungibleToken token = new NonFungibleTokenBuilder()
                    .ofTokenType(state.toPointer())
                    .issuedBy(getOurIdentity())
                    .heldBy(recipient)
                    .buildNonFungibleToken();

            return subFlow(new IssueTokens(Arrays.asList(token)));
        }
    }

}
