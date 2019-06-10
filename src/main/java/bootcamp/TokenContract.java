package bootcamp;

import net.corda.core.contracts.CommandData;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class TokenContract {
    public static String ID = "bootcamp.TokenContract";



    public interface Commands extends CommandData {
        class Issue implements Commands { }
    }
}