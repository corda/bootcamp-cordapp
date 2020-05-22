package bootcamp;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class TokenContract implements Contract {
    public static String ID = "bootcamp.TokenContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        if(tx.getInputStates().size() != 0 ) throw new IllegalArgumentException("Zero Inputs Expected");
        if(tx.getOutputStates().size() != 1) throw new IllegalArgumentException("One Output Expected");
        if(tx.getCommands().size() != 1) throw new IllegalArgumentException("One command expected");
        if(!(tx.getOutput(0) instanceof TokenState)) throw new IllegalArgumentException("Output of type TokenState expected");
        if(!(tx.getCommand(0).getValue() instanceof Commands.Issue)) throw new IllegalArgumentException("Issue Command expected");
        TokenState tokenState = (TokenState)tx.getOutput(0);
        if(tokenState.getAmount() < 1) throw new IllegalArgumentException("Amount must be positive");
    }

    public interface Commands extends CommandData {
        class Issue implements Commands { }
        //class Move implements Commands { }
        //class Transfer implements Commands{}
    }
}