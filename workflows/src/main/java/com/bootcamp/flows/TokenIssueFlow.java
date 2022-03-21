package com.bootcamp.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.bootcamp.contracts.TokenContract;
import com.bootcamp.states.TokenState;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.contracts.CommandData;

import static java.util.Collections.singletonList;

public class TokenIssueFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class TokenIssueFlowInitiator extends FlowLogic<SignedTransaction> {
        private final Party owner;
        private final int amount;

        public TokenIssueFlowInitiator(Party owner, int amount) {
            this.owner = owner;
            this.amount = amount;
        }

        private final ProgressTracker progressTracker = new ProgressTracker();

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
            // We get a reference to our own identity.
            Party issuer = getOurIdentity();

            /* ============================================================================
             *         TODO 1 - Create our TokenState to represent on-ledger tokens!
             * ===========================================================================*/
            // We create our new TokenState.
            TokenState tokenState = null;

            /* ============================================================================
             *      TODO 3 - Build our token issuance transaction to update the ledger!
             * ===========================================================================*/
            // We build our transaction.
            TransactionBuilder transactionBuilder = null;

            /* ============================================================================
             *          TODO 2 - Write our TokenContract to control token issuance!
             * ===========================================================================*/
            // We check our transaction is valid based on its contracts.
            transactionBuilder.verify(getServiceHub());

            FlowSession session = initiateFlow(owner);

            // We sign the transaction with our private key, making it immutable.
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            // The counterparty signs the transaction
            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, singletonList(session)));

            // We get the transaction notarised and recorded automatically by the platform.
            return subFlow(new FinalityFlow(fullySignedTransaction, singletonList(session)));
        }
    }

    @InitiatedBy(TokenIssueFlowInitiator.class)
    public static class TokenIssueFlowResponder extends FlowLogic<Void>{
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public TokenIssueFlowResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    /*
                     * SignTransactionFlow will automatically verify the transaction and its signatures before signing it.
                     * However, just because a transaction is contractually valid doesn’t mean we necessarily want to sign.
                     * What if we don’t want to deal with the counterparty in question, or the value is too high,
                     * or we’re not happy with the transaction’s structure? checkTransaction
                     * allows us to define these additional checks. If any of these conditions are not met,
                     * we will not sign the transaction - even if the transaction and its signatures are contractually valid.
                     * ----------
                     * For this hello-world cordapp, we will not implement any aditional checks.
                     * */
                }
            });
            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }
}