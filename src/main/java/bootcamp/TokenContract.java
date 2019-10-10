package bootcamp;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class TokenContract implements Contract {

    public static String ID = "bootcamp.TokenContract";

    public interface Commands extends CommandData {
        class Issue implements Commands { }
    }

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), TokenContract.Commands.class);

        List<ContractState> inputs = tx.getInputStates();
        List<ContractState> outputs = tx.getOutputStates();

        if(command.getValue() instanceof Commands.Issue) {

            // Shape
            if(inputs.size() != 0) throw new IllegalArgumentException("Input size must be 0");
            if(outputs.size() != 1) throw new IllegalArgumentException("Output size must be 1");
            if(tx.getCommands().size() != 1) throw new IllegalArgumentException("Commands size must be 1");

            // Content
            if(tx.outputsOfType(TokenState.class).size() != 1) throw new IllegalArgumentException("This contract only accept TokenState");
            if(((TokenState) tx.getOutput(0)).getAmount() <= 0 ) throw new IllegalArgumentException("This contract only allow positive TokeState value");
            if(tx.commandsOfType(Commands.Issue.class).size() != 1) throw new IllegalArgumentException("This contract only allow Issue command");

            // Notary
            List<PublicKey> signers = tx.getCommand(0).getSigners();
            PublicKey issuer = ((TokenState) tx.getOutput(0)).getIssuer().getOwningKey();
            if(!signers.contains(issuer)) throw new IllegalArgumentException("This contract require Issuer as Signer");

        } else throw new IllegalArgumentException("Unrecognised command.");

    }


}