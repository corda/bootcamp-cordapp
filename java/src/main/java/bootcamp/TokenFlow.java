package bootcamp;

import bootcamp.solution.TokenContract;
import bootcamp.solution.TokenState;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

/* Our flow, automating the process of updating the ledger. */
@InitiatingFlow
@StartableByRPC
public class TokenFlow extends FlowLogic<Void> {
    private final ProgressTracker progressTracker = new ProgressTracker();
    private final Party recipient;
    private final int amount;

    public TokenFlow(Party recipient, int amount) {
        this.recipient = recipient;
        this.amount = amount;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // We choose our transaction's notary (the notary prevents double-spends).
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // We build our transaction.
        TransactionBuilder txBuilder = new TransactionBuilder(notary);
        txBuilder.addOutputState(new TokenState(getOurIdentity(), recipient, amount), TokenContract.ID);
        txBuilder.addCommand(new TokenContract.Issue(), getOurIdentity().getOwningKey());

        // We check our transaction is valid based on its contracts.
        txBuilder.verify(getServiceHub());

        // We sign the transaction with our public key, making it immutable.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // We get the transaction notarised and recorded automatically by the platform.
        subFlow(new FinalityFlow(signedTx));

        return null;
    }
}
