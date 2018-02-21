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
class TokenState

/* Our contract, governing how our state will evolve over time.
 * See src/main/kotlin/examples/ExampleContract.kt for an example. */
class TokenContract

/* Our flow, automating the process of updating the ledger.
 * See src/main/kotlin/examples/ExampleFlow.kt for an example. */
@InitiatingFlow
@StartableByRPC
class TokenFlow(val recipient: Party, val amount: Int) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        // We choose our transaction's notary (the notary prevents double-spends).
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // We build our transaction.
        val transactionBuilder: TransactionBuilder = TODO("Build a valid transaction.")

        // We check our transaction is valid based on its contracts.
        transactionBuilder.verify(serviceHub)

        // We sign the transaction with our public key, making it immutable.
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        // We get the transaction notarised and recorded automatically by the platform.
        subFlow(FinalityFlow(signedTransaction))
    }
}