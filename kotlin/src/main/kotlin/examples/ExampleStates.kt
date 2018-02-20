package examples

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

// The simplest form of state.
// Implements ContractState (all states must implement ContractState or a sub-interface).
class IAmAState: ContractState {
    // Overrides participants, the only field defined by ContractState.
    override val participants = listOf<Party>()
}

// A much more complex state.
data class IAmAlsoAState(
        // Defines various fields that will be stored on the ledger as part of the state.
        val data: String,
        val person: Party,
        // Also overrides linearId, a field defined by LinearState, and provides a default value.
        override val linearId: UniqueIdentifier = UniqueIdentifier()
// Doesn't implement ContractState directly. Instead, implements LinearState which extends ContractState.
// Can also implement other interfaces (in this case, Comparable).
): LinearState, Comparable<IAmAlsoAState> {
    // Overrides participants, the only field defined by ContractState.
    override val participants = listOf(person)

    // Can implement additional functions as well.
    override fun compareTo(other: IAmAlsoAState): Int {
        return linearId.compareTo(other.linearId)
    }
}