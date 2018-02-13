package bootcamp

import bootcamp.solution.TokenContract
import bootcamp.solution.TokenState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/* Our state, defining a shared fact on the ledger.
 * See src/main/kotlin/examples/ExampleStates.kt for examples. */
class TokenState

/* Our contract, governing how our state will evolve over time.
 * See src/main/kotlin/examples/ExampleContract.kt for an example. */
class TokenContract

/* Our flow, automating the process of updating the ledger. */
@InitiatingFlow
@StartableByRPC
class TokenFlow(val recipient: Party, val amount: Int) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        // We choose our transaction's notary (the notary prevents double-spends).
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // We build our transaction.
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addOutputState(TokenState(ourIdentity, recipient, amount), TokenContract.ID)
        txBuilder.addCommand(TokenContract.Issue, ourIdentity.owningKey)

        // We check our transaction is valid based on its contracts.
        txBuilder.verify(serviceHub)

        // We sign the transaction with our public key, making it immutable.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // We get the transaction notarised and recorded automatically by the platform.
        subFlow(FinalityFlow(signedTx))
    }
}
