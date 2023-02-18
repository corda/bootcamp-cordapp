# TokenState

```java
package com.bootcamp.states;

import com.bootcamp.contracts.TokenContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* Our state, defining a shared fact on the ledger.
 * See src/main/java/examples/ArtState.java for an example. */
@BelongsToContract(TokenContract.class)
public class TokenState implements ContractState {

    private final Party issuer;
    private final Party owner;
    private final int amount;
    private final List<AbstractParty> participants;

    public TokenState(Party issuer, Party owner, int amount) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
        this.participants = new ArrayList<>();
        participants.add(issuer);
        participants.add(owner);
    }

    public Party getIssuer() {
        return issuer;
    }

    public Party getOwner() {
        return owner;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    @NotNull
    public List<AbstractParty> getParticipants() {
        return participants;
    }
}

```

# TokenContract

```java
package com.bootcamp.contracts;

import com.bootcamp.states.TokenState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class TokenContract implements Contract {
    public static String ID = "com.bootcamp.contracts.TokenContract";

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {

        CommandWithParties<TokenContract.Commands> command = requireSingleCommand(tx.getCommands(), TokenContract.Commands.class);

        List<ContractState> inputs = tx.getInputStates();
        List<ContractState> outputs = tx.getOutputStates();

        if (command.getValue() instanceof TokenContract.Commands.Issue) {
            requireThat(req -> {
                req.using("Transaction must have no input states.", inputs.isEmpty());
                req.using("Transaction must have exactly one output.", outputs.size() == 1);
                req.using("Output must be a TokenState.", outputs.get(0) instanceof TokenState);
                TokenState output = (TokenState) outputs.get(0);
                req.using("Issuer must be required singer.", command.getSigners().contains(output.getIssuer().getOwningKey()));
                req.using("Owner must be required singer.", command.getSigners().contains(output.getOwner().getOwningKey()));
                req.using("Amount must be positive.", output.getAmount() > 0);
                return null;
            });
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    public interface Commands extends CommandData {
        class Issue implements Commands {
        }
    }
}
```

# TokenIssueFlowInitiator

```java
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

        Party issuer = getOurIdentity();

        TokenState tokenState = new TokenState(issuer, owner, amount);

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);

        CommandData commandData = new TokenContract.Commands.Issue();

        transactionBuilder.addCommand(commandData, issuer.getOwningKey(), owner.getOwningKey());

        transactionBuilder.addOutputState(tokenState, TokenContract.ID);

        transactionBuilder.verify(getServiceHub());

        FlowSession session = initiateFlow(owner);

        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, singletonList(session)));

        return subFlow(new FinalityFlow(fullySignedTransaction, singletonList(session)));
    }
}
```

# Running our Nodes

```sh
./gradlew clean deploynodes

#(OPTIONAL)
killall -9 java

./build/nodes/runnodes
flow start TokenIssueFlowInitiator owner: PartyB, amount: 50
```

# via docker 
```sh
./gradlew clean prepareDockerNodes

ACCEPT_LICENSE=Y | docker-compose -f ./build/nodes/docker-compose.yml up


```

