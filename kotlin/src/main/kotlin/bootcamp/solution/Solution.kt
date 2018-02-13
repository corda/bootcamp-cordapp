package bootcamp.solution

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

/* Our state, defining a shared fact on the ledger. */
class TokenState(val issuer: Party, val recipient: Party, val amount: Int) : ContractState {
    override val participants = listOf(issuer, recipient)
}

/* Our contract, governing how our state will evolve over time. */
class TokenContract : Contract {
    companion object {
        val ID = "bootcamp.solution.TokenContract"
    }

    object Issue: CommandData

    override fun verify(tx: LedgerTransaction) = requireThat {
        "Transaction should have no inputs" using (tx.inputStates.size == 0)
        "Transaction should have one output" using (tx.outputStates.size == 1)
        "Transaction should have one command" using (tx.commands.size == 1)

        val outputState = tx.outputStates[0]
        "Output should be a TokenState" using (outputState is TokenState)
        outputState as TokenState
        "Token amount should be positive" using (outputState.amount > 0)

        val command = tx.commands[0]
        "Command should be Issue" using (command.value is Issue)
        "Issuer must sign the issuance" using (outputState.issuer.owningKey in command.signers)
    }
}