package java_examples;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;

import java.util.List;

// The simplest form of state.
// Implements ContractState (all states must implement ContractState or a sub-interface).
public class IAmAState implements ContractState {
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of();
    }
}