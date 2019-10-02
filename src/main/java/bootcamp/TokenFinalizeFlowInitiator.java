package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;

import static java.util.Collections.singletonList;

@InitiatingFlow
@StartableByRPC
public class TokenFinalizeFlowInitiator extends FlowLogic<SignedTransaction> {
    private StateAndRef<TokenState> token;
    private final String txHash;
    private final int index;

    public TokenFinalizeFlowInitiator(StateAndRef<TokenState> token) {
        this.token = token;
        this.txHash = null;
        this.index = 0;
    }
    // override for running from shell
    public TokenFinalizeFlowInitiator(String txHash, int index) {
        this.txHash = txHash;
        this.index = index;
    }

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() { return progressTracker; }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        if (token == null) {
            SecureHash hash = SecureHash.parse(txHash);
            token = getServiceHub().toStateAndRef(new StateRef(hash, index));
        }

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Party owner = getOurIdentity();
        Party issuer = token.getState().getData().getIssuer();

        CommandData commandData = new TokenContract.Commands.Finalize();

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
        transactionBuilder.addCommand(commandData, Arrays.asList(issuer.getOwningKey(), owner.getOwningKey()));
        transactionBuilder.addInputState(token);
        transactionBuilder.verify(getServiceHub());

        FlowSession issuerSession = initiateFlow(issuer);

        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);
        SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, singletonList(issuerSession)));

        return subFlow(new FinalityFlow(fullySignedTransaction, singletonList(issuerSession)));
    }
}