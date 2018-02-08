package bootcamp

import bootcamp.TokenContract.Issue
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/* Our state, defining a shared fact on the ledger. */
data class TokenState(val issuer: Party, val recipient: Party, val amount: Int) : ContractState {
    override val participants = listOf(issuer, recipient)
}

/* Our contract, governing how our state will evolve over time. */
class TokenContract : Contract {
    companion object {
        val ID = "bootcamp.TokenContract"
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

/* Our flow, automating the process of updating the ledger. */
@InitiatingFlow
@StartableByRPC
class TokenFlow(val charity: Party, val amount: Int) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        // We choose our transaction's notary (the notary prevents double-spends).
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // We build our transaction.
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addOutputState(TokenState(ourIdentity, charity, amount), TokenContract.ID)
        txBuilder.addCommand(Issue, ourIdentity.owningKey)

        // We check our transaction is valid based on its contracts.
        txBuilder.verify(serviceHub)

        // We sign the transaction with our public key, making it immutable.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // We get the transaction notarised and recorded automatically by the platform.
        subFlow(FinalityFlow(signedTx))
    }
}