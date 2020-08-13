package bootcamp;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/* Our state, defining a shared fact on the ledger.
 * See src/main/java/examples/ArtState.java for an example. */
@BelongsToContract(TokenContract.class)
public class TokenState implements ContractState{

    private final AbstractParty issuer;
    private final AnonymousParty owner;
    private final int amount;

    public TokenState(AbstractParty issuer, AnonymousParty owner, int amount) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
    }

    public AbstractParty getIssuer() {
        return issuer;
    }

    public AbstractParty getOwner() {
        return owner;
    }

    public int getAmount() {
        return amount;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(issuer,owner);
    }

    @Override
    public String toString() {
        return "\n" +
                "TokenState{" + "\n" +
                "issuer=" + issuer + "\n" +
                ", owner=" + owner + "\n" +
                ", amount=" + amount + "\n" +
                "}";
    }
}