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
data class DonationState(val donor: Party, val charity: Party, val amount: Int) : ContractState {
    override val participants = listOf(donor, charity)
}

/* Our contract, governing how our state will evolve over time. */
class DonationContract : Contract {
    companion object {
        val DONATION_CONTRACT_ID = "bootcamp.DonationContract"
    }

    object Donate: CommandData

    override fun verify(tx: LedgerTransaction) {
        if (tx.inputStates.size != 0) throw Exception("Transaction should have no inputs")
        if (tx.outputStates.size != 1) throw Exception("Transaction should have one output")
        if (tx.commands.size != 1) throw Exception("Transaction should have one command")

        val outputState = tx.outputStates[0]
        if (outputState !is DonationState) throw Exception("Output should be a DonationState")
        if (outputState.amount < 0) throw Exception("Donation should be positive")
        
        val command = tx.commands[0]
        if (command.value !is Donate) throw Exception("Command should be Donate")
        if (outputState.donor.owningKey !in command.signers) throw Exception( "Donor must sign the donation")
    }
}

/* Our flow, automating the process of updating the ledger. */
@InitiatingFlow
@StartableByRPC
class DonationFlow(val charity: Party, val amount: Int) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        // We choose our transaction's notary (the notary prevents double-spends).
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // We build our transaction.
        val txBuilder = TransactionBuilder(notary)
        val outputState = DonationState(ourIdentity, charity, amount)
        val outputStateAndContract = StateAndContract(outputState, DonationContract.DONATION_CONTRACT_ID)
        val command = Command(DonationContract.Donate, ourIdentity.owningKey)
        txBuilder.withItems(outputStateAndContract, command)

        // We check our transaction is valid based on its contracts.
        txBuilder.verify(serviceHub)

        // We sign the transaction with our public key, making it immutable.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // We get the transaction notarised and recorded automatically by the platform.
        subFlow(FinalityFlow(signedTx))
    }
}