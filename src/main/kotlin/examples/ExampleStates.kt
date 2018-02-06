package examples

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

// The simplest form of state.
// I implement ContractState (all states must implement ContractState or a sub-interface).
class IAmAState: ContractState {
    // I override participants, the only field defined by ContractState.
    override val participants = listOf<Party>()
}

// A much more complex state.
class IAmAlsoAState(
        // I define various fields that will be stored on the ledger as part of the state.
        val data: String,
        val person: Party,
        // I also override linearId, a field defined by LinearState.
        override val linearId: UniqueIdentifier
// I don't implement ContractState directly. Instead, I implement LinearState which extends ContractState.
// I can also implement other interfaces (in this case, Comparable).
): LinearState, Comparable<IAmAlsoAState> {
    // I override participants, the only field defined by ContractState.
    override val participants = listOf<Party>()

    // I can implement any additional functions I like.
    override fun compareTo(other: IAmAlsoAState): Int {
        return when {
            linearId == other.linearId -> 0
            linearId > other.linearId -> 1
            else -> -1
        }
    }
}