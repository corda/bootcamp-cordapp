package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;

@InitiatingFlow
@StartableByRPC
public class TokenTransferFlowInitiator extends FlowLogic<SignedTransaction> {
    private StateAndRef<TokenState> token;
    private final Party target;
    private final int amount;
    private final String txHash;

    public TokenTransferFlowInitiator(StateAndRef<TokenState> token, Party target, int amount) {
        this.token = token;
        this.target = target;
        this.amount = amount;
        this.txHash = null;
    }
    // overload for running from shell
    public TokenTransferFlowInitiator(String txHash, Party target, int amount) throws TransactionResolutionException {
        this.token = null;
        this.txHash = txHash;
        this.target = target;
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
        if (token == null) {
            SecureHash hash = SecureHash.parse(txHash);
            token = getServiceHub().toStateAndRef(new StateRef(hash, 0));
        }

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Party owner = getOurIdentity();
        Party issuer = token.getState().getData().getIssuer();
        int tokenValue = token.getState().getData().getAmount();

        // FLOW CHECKS
        // Amount must not exceed value in token
        if (amount > tokenValue) throw new FlowException("Amount higher than token value");
        // Token must be owned by party wishing to propose transaction
        if (!token.getState().getData().getOwner().equals(owner)) throw new FlowException("Party doesn't own token being transferred");

        CommandData commandData = new TokenContract.Commands.Transfer();

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
        transactionBuilder.addCommand(commandData, Arrays.asList(issuer.getOwningKey(), owner.getOwningKey(), target.getOwningKey()));
        transactionBuilder.addInputState(token);
        transactionBuilder.addOutputState(new TokenState(issuer, target, amount));
        if (amount < tokenValue) { // assign remaining value back to owner
            transactionBuilder.addOutputState(new TokenState(issuer, owner, (tokenValue-amount)));
        }
        transactionBuilder.verify(getServiceHub());

        FlowSession targetSession = initiateFlow(target); // target counter-party
        FlowSession issuerSession = initiateFlow(issuer); // original issuer

        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);
        SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, Arrays.asList(issuerSession, targetSession)));

        return subFlow(new FinalityFlow(fullySignedTransaction, Arrays.asList(issuerSession, targetSession)));
    }
}
