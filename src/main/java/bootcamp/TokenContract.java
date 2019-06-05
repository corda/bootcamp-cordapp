package bootcamp;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class TokenContract implements Contract {
    public static String ID = "bootcamp.TokenContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        List<ContractState> inputs = tx.getInputStates();
        List<ContractState> outputs = tx.getOutputStates();

        CommandWithParties<TokenContract.Commands> command = requireSingleCommand(tx.getCommands(), TokenContract.Commands.class);

        if (command.component3() instanceof TokenContract.Commands.Issue) {
            if (!inputs.isEmpty()) {
                throw new IllegalArgumentException("No inputs!");
            }
            if (outputs.size() != 1) {
                throw new IllegalArgumentException("output!");
            }
            if (!(outputs.get(0) instanceof TokenState)) {
                throw new IllegalArgumentException("must be tokenstate");
            }
            TokenState output = (TokenState) outputs.get(0);
            if (!command.getSigners().contains(output.getIssuer().getOwningKey())) {
                throw new IllegalArgumentException("issuer must be signer");
            }
            if (output.getAmount() < 1) {
                throw new IllegalArgumentException("asdf");
            }
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    public interface Commands extends CommandData {
        class Issue implements Commands { }
    }
}