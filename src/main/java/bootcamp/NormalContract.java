package bootcamp;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class NormalContract implements Contract {

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {

    }

    public interface Commands extends CommandData {
        class Issue implements Commands { }
        class TransferAsset implements Commands { }

        //class Move implements Commands { }
        //class Transfer implements Commands{}

    }
}