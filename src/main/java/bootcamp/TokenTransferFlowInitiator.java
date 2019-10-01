package bootcamp;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;

import static java.util.Collections.singletonList;

@InitiatingFlow
@StartableByRPC
public class TokenTransferFlowInitiator extends FlowLogic<SignedTransaction> {
    private final StateAndRef<TokenState> token;
    private final Party target;
    private final int amount;

    public TokenTransferFlowInitiator(StateAndRef<TokenState> token, Party target, int amount) {
        this.token = token;
        this.target = target;
        this.amount = amount;
    }

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Override
    public SignedTransaction call() throws FlowException {

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Party owner = getOurIdentity();
        Party issuer = token.getState().getData().getIssuer();
        int tokenValue = token.getState().getData().getAmount();

        // Amount must not exceed value in token
        if (amount > tokenValue) throw new FlowException("Amount higher than token value");

        CommandData commandData = new TokenContract.Commands.Transfer();

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
        transactionBuilder.addCommand(commandData, Arrays.asList(issuer.getOwningKey(), owner.getOwningKey(), target.getOwningKey()));
        transactionBuilder.addInputState(token);
        transactionBuilder.addOutputState(new TokenState(issuer, target, amount));
        if (amount < tokenValue) { // assign remaining value back to owner
            transactionBuilder.addOutputState(new TokenState(issuer, owner, (tokenValue-amount)));
        }
        transactionBuilder.verify(getServiceHub());

        FlowSession session = initiateFlow(owner);

        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);
        SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, singletonList(session)));

        return subFlow(new FinalityFlow(fullySignedTransaction, singletonList(session)));
    }
}
