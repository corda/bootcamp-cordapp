package bootcamp

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
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/* Our state, defining a shared fact on the ledger.
 * See src/main/kotlin/examples/ExampleStates.kt for examples. */
class TokenState(val issuer: Party, val recipient: Party, val amount: Int) : ContractState {
    override val participants = listOf(issuer, recipient)
}

/* Our contract, governing how our state will evolve over time.
 * See src/main/kotlin/examples/ExampleContract.kt for an example. */
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
        "Command should be Issue" using (command.value is TokenContract.Issue)
        "Issuer must sign the issuance" using (outputState.issuer.owningKey in command.signers)
    }
}

/* Our flow, automating the process of updating the ledger.
 * See src/main/kotlin/examples/ExampleFlow.kt for an example. */
@InitiatingFlow
@StartableByRPC
class TokenFlow(val recipient: Party, val amount: Int) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // We choose our transaction's notary (the notary prevents double-spends).
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // We build our transaction.
        val transactionBuilder = TransactionBuilder(notary)
        transactionBuilder.addOutputState(TokenState(ourIdentity, recipient, amount), TokenContract.ID)
        transactionBuilder.addCommand(TokenContract.Issue, ourIdentity.owningKey)

        // We check our transaction is valid based on its contracts.
        transactionBuilder.verify(serviceHub)

        // We sign the transaction with our public key, making it immutable.
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // We get the transaction notarised and recorded automatically by the platform.
        return subFlow(FinalityFlow(signedTransaction))
    }
}
