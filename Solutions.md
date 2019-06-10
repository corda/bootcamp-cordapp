# TokenState

    package java_bootcamp;

    import com.google.common.collect.ImmutableList;
    import net.corda.core.contracts.ContractState;
    import net.corda.core.identity.AbstractParty;
    import net.corda.core.identity.Party;

    import java.util.List;

    /* Our state, defining a shared fact on the ledger.
     * See src/main/java/examples/ArtState.java for an example. */
    public class TokenState implements ContractState {
        private final Party issuer;
        private final Party owner;
        private final int amount;

        public Party getIssuer() {
            return issuer;
        }

        public Party getOwner() {
            return owner;
        }

        public int getAmount() {
            return amount;
        }

        public TokenState(Party issuer, Party owner, int amount) {
            this.issuer = issuer;
            this.owner = owner;
            this.amount = amount;
        }

        @Override
        public List<AbstractParty> getParticipants() {
            return ImmutableList.of(issuer, owner);
        }
    }

# TokenContract

    package java_bootcamp;

    import net.corda.core.contracts.Command;
    import net.corda.core.contracts.CommandData;
    import net.corda.core.contracts.Contract;
    import net.corda.core.contracts.ContractState;
    import net.corda.core.transactions.LedgerTransaction;

    import java.security.PublicKey;

    /* Our contract, governing how our state will evolve over time.
     * See src/main/java/examples/ArtContract.java for an example. */
    public class TokenContract implements Contract {
        public static final String ID = "java_bootcamp.TokenContract";

        public interface Commands extends CommandData {
            class Issue implements Commands {
            }
        }

        @Override
        public void verify(LedgerTransaction tx) throws IllegalArgumentException {
            if (tx.getInputStates().size() != 0) throw new IllegalArgumentException("Transaction should have no inputs");
            if (tx.getOutputStates().size() != 1) throw new IllegalArgumentException("Transaction should have one output");
            if (tx.getCommands().size() != 1) throw new IllegalArgumentException("Transaction should have one command");

            ContractState outputState = tx.getOutput(0);
            if (!(outputState instanceof TokenState)) throw new IllegalArgumentException("Output should be a TokenState");
            TokenState outputTokenState = (TokenState) outputState;
            if (outputTokenState.getAmount() <= 0) throw new IllegalArgumentException("Token amount should be positive");

            Command<CommandData> command = tx.getCommand(0);
            if (!(command.getValue() instanceof TokenContract.Commands.Issue))
                throw new IllegalArgumentException("Command should be Issue");
            PublicKey issuerPublicKey = outputTokenState.getIssuer().getOwningKey();
            if (!(command.getSigners().contains(issuerPublicKey)))
                throw new IllegalArgumentException("Issuer must sign the issuance");
        }
    }

# TokenIssueFlow

    package java_bootcamp;

    import co.paralleluniverse.fibers.Suspendable;
    import net.corda.core.flows.*;
    import net.corda.core.identity.Party;
    import net.corda.core.transactions.SignedTransaction;
    import net.corda.core.transactions.TransactionBuilder;
    import net.corda.core.utilities.ProgressTracker;

    /* Our flow, automating the process of updating the ledger.
    * See src/main/java/examples/ArtTransferFlowInitiator.java for an example. */
    @InitiatingFlow
    @StartableByRPC
    public class TokenIssueFlow extends FlowLogic<SignedTransaction> {
        private final ProgressTracker progressTracker = new ProgressTracker();
        private final Party owner;
        private final int amount;

        public TokenIssueFlow(Party owner, int amount) {
            this.owner = owner;
            this.amount = amount;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // We choose our transaction's notary (the notary prevents double-spends).
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            // We get a reference to our own identity.
            Party issuer = getOurIdentity();

            // We create our new TokenState.
            TokenState tokenState = new TokenState(issuer, owner, amount);

            // We build our transaction.
            TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
            transactionBuilder.addOutputState(tokenState, TokenContract.ID);
            transactionBuilder.addCommand(new TokenContract.Commands.Issue(), getOurIdentity().getOwningKey());

            // We check our transaction is valid based on its contracts.
            transactionBuilder.verify(getServiceHub());

            // We sign the transaction with our private key, making it immutable.
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            // We get the transaction notarised and recorded automatically by the platform.
            return subFlow(new FinalityFlow(signedTransaction));
        }
    }