package bootcamp.flows;

import bootcamp.contracts.IOUContract;
import bootcamp.states.IOUState;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Arrays;

import static java.util.Collections.singletonList;

public class IssueIOUFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class IssueIOUFlowInitiator extends FlowLogic<SignedTransaction>{

        //private variables
        private int iouValue ;
        private Party borrower;

        //public constructor
        public IssueIOUFlowInitiator(int iouValue, Party borrower) {
            this.iouValue = iouValue;
            this.borrower = borrower;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party lender = getOurIdentity();

            // Get a reference to the notary service on our network and our key pair.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);


            // TODO:  Compose the IOU State

            // TODO: Build the transaction
            TransactionBuilder transactionBuilder = null;

            //  Verify and sign it with our KeyPair.
            transactionBuilder.verify(getServiceHub());

            FlowSession session = initiateFlow(borrower);

            // We sign the transaction with our private key, making it immutable.
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            // The counterparty signs the transaction
            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, singletonList(session)));

            // We get the transaction notarised and recorded automatically by the platform.
            return subFlow(new FinalityFlow(fullySignedTransaction, singletonList(session)));
        }
    }

    @InitiatedBy(IssueIOUFlowInitiator.class)
    public static class IssueIOUFlowResponder extends FlowLogic<Void>{
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public IssueIOUFlowResponder(FlowSession counterpartySession) {
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
