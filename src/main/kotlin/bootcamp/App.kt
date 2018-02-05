package bootcamp

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/* Our state, defining a shared fact on the ledger. */
data class SomethingState(val owner: Party) : ContractState {
    override val participants = listOf(owner)
}

/* Our contract, governing how our state will evolve over time. */
class SomethingContract : Contract {
    companion object {
        val SOMETHING_CONTRACT_ID = "bootcamp.SomethingContract"
    }

    class SomethingCommand: CommandData

    override fun verify(tx: LedgerTransaction) {
        if (tx.inputs.isNotEmpty()) throw Exception("Transaction has no inputs")
        if (tx.outputs.size != 1) throw Exception("Transaction has one output")
        if (tx.commands.size != 1) throw Exception("Transaction has one command")
        val outputState = tx.outputsOfType<SomethingState>().single()
        val command = tx.commands.single()
        if (outputState.owner.owningKey !in command.signers) throw Exception( "Command lists output's two parties as signers")
    }
}

/* Our flow, automating the process of updating the ledger. */
@InitiatingFlow
@StartableByRPC
class SomethingFlow : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        // We choose our transaction's notary (the notary prevents double-spends).
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // We build our transaction.
        val txBuilder = TransactionBuilder(notary)
        val outputState = SomethingState(ourIdentity)
        val outputStateAndContract = StateAndContract(outputState, SomethingContract.SOMETHING_CONTRACT_ID)
        val command = Command(SomethingContract.SomethingCommand(), ourIdentity.owningKey)
        txBuilder.withItems(outputStateAndContract, command)

        // We check our transaction is valid based on its contracts.
        txBuilder.verify(serviceHub)

        // We sign the transaction with our public key, making it immutable.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // We get the transaction notarised and recorded automatically by the platform.
        subFlow(FinalityFlow(signedTx))
    }
}