package bootcamp;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.List;

///* Our state, defining a shared fact on the ledger.
// * See src/main/kotlin/examples/IAmAState.java and
// * src/main/kotlin/examples/IAmAlsoAState.java for examples. */
//public class TokenState {
//
//}

/* SOLUTION */
public class TokenState implements ContractState {
    private final Party issuer;
    private final Party recipient;
    private final int amount;

    public Party getIssuer() {
        return issuer;
    }

    public Party getRecipient() {
        return recipient;
    }

    public int getAmount() {
        return amount;
    }

    public TokenState(Party issuer, Party recipient, int amount) {
        this.issuer = issuer;
        this.recipient = recipient;
        this.amount = amount;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(issuer, recipient);
    }
}