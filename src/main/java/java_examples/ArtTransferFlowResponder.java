package java_examples;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

// `InitiatedBy` means that we will start this flow in response to a
// message from `ArtTransferFlow.Initiator`.
@InitiatedBy(ArtTransferFlowInitiator.class)
public class ArtTransferFlowResponder extends FlowLogic<Void> {
    private final FlowSession counterpartySession;

    // Responder flows always have a single constructor argument - a
    // `FlowSession` with the counterparty who initiated the flow.
    public ArtTransferFlowResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // If the counterparty requests our signature on a transaction using
        // `CollectSignaturesFlow`, we need to respond by invoking our own
        // `SignTransactionFlow` subclass.
        class SignTxFlow extends SignTransactionFlow {
            private SignTxFlow(FlowSession otherSession, ProgressTracker progressTracker) {
                super(otherSession, progressTracker);
            }

            // As part of `SignTransactionFlow`, the contracts of the
            // transaction's input and output states are run automatically.
            // Inside `checkTransaction`, we define our own additional logic
            // for checking the received transaction. If `checkTransaction`
            // throws an exception, we'll refuse to sign.
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                // Whatever checking you want to do...
            }
        }

        subFlow(new SignTxFlow(counterpartySession, SignTransactionFlow.tracker()));

        // Once the counterparty calls `FinalityFlow`, we will
        // automatically record the transaction if we are one of the
        // `participants` on one or more of the transaction's states.

        return null;
    }
}