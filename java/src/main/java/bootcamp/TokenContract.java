package bootcamp;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireThat;

///* Our contract, governing how our state will evolve over time.
// * See src/main/kotlin/examples/ExampleContract.java for an example. */
//public class TokenContract {
//
//}

/* SOLUTION */
public class TokenContract implements Contract {
    public static final String ID = "bootcamp.TokenContract";

    public static class Issue implements CommandData {
    }

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        requireThat(require -> {
            require.using("Transaction should have no inputs", tx.getInputStates().size() == 0);
            require.using("Transaction should have one output", tx.getOutputStates().size() == 1);
            require.using("Transaction should have one command", tx.getCommands().size() == 1);

            ContractState outputState = tx.getOutput(0);
            require.using("Output should be a TokenState", outputState instanceof TokenState);
            TokenState outputTokenState = (TokenState) outputState;
            require.using("Token amount should be positive", outputTokenState.getAmount() > 0);

            Command<CommandData> command = tx.getCommand(0);
            require.using("Command should be Issue", command.getValue() instanceof bootcamp.TokenContract.Issue);
            require.using("Issuer must sign the issuance",
                    command.getSigners().contains(outputTokenState.getIssuer().getOwningKey()));

            return null;
        });
    }
}