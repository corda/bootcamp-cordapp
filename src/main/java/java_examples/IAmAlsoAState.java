package java_examples;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.List;

// A much more complex state.
// Doesn't implement ContractState directly. Instead, implements LinearState which extends ContractState.
// Can also implement other interfaces (in this case, Comparable).
public class IAmAlsoAState implements LinearState, Comparable<IAmAlsoAState> {
    // Defines various fields that will be stored on the ledger as part of the state.
    private final String data;
    private final Party person;
    // Also overrides linearId, a field defined by LinearState.
    private final UniqueIdentifier linearId;

    public IAmAlsoAState(String data, Party person, UniqueIdentifier linearId) {
        this.data = data;
        this.person = person;
        this.linearId = linearId;
    }

    public String getData() {
        return data;
    }

    public Party getPerson() {
        return person;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    // Overrides participants, the only field defined by ContractState.
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(person);
    }

    // Can implement additional functions as well.
    @Override
    public int compareTo(IAmAlsoAState other) {
        return linearId.compareTo(other.linearId);
    }
}