package bootcamp;

import net.corda.core.contracts.CommandData;
import net.corda.core.transactions.LedgerTransaction;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;    
import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.util.List;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class TokenContract {
    public static String ID = "bootcamp.TokenContract";


    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
    
    }


    public interface Commands extends CommandData {
        class Issue implements Commands { }
    }
}
