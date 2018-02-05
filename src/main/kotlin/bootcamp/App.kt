package bootcamp

import bootcamp.TokenContract.Companion.TOKEN_CONTRACT_ID
import bootcamp.TokenContract.Issue
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
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
        val TOKEN_CONTRACT_ID = "bootcamp.TokenContract"
    }

    object Issue: CommandData

    override fun verify(tx: LedgerTransaction) {
        if (tx.inputStates.size != 0) throw Exception("Transaction should have no inputs")
        if (tx.outputStates.size != 1) throw Exception("Transaction should have one output")
        if (tx.commands.size != 1) throw Exception("Transaction should have one command")

        val outputState = tx.outputStates[0]
        if (outputState !is TokenState) throw Exception("Output should be a TokenState")
        if (outputState.amount < 0) throw Exception("Token amount should be positive")

        val command = tx.commands[0]
        if (command.value !is Issue) throw Exception("Command should be Donate")
        if (outputState.issuer.owningKey !in command.signers) throw Exception( "Donor must sign the issuance")
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
        txBuilder.addOutputState(TokenState(ourIdentity, charity, amount), TOKEN_CONTRACT_ID)
        txBuilder.addCommand(Issue, ourIdentity.owningKey)

        // We check our transaction is valid based on its contracts.
        txBuilder.verify(serviceHub)

        // We sign the transaction with our public key, making it immutable.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // We get the transaction notarised and recorded automatically by the platform.
        subFlow(FinalityFlow(signedTx))
    }
}