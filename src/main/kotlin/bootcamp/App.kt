package bootcamp

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.*
import net.corda.core.utilities.ProgressTracker

/* Contract name **/
val SOMETHING_CONTRACT_ID = "bootcamp.SomethingContract"

/* State **/
data class SomethingState(val owner: Party) : ContractState {
    override val participants = listOf(owner)
}

/* Contract **/
class SomethingContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        if (tx.inputs.isNotEmpty()) throw Exception("Transaction has no inputs")
        if (tx.outputs.size != 1) throw Exception("Transaction has one output")
        if (tx.commands.size != 1) throw Exception("Transaction has one command")
        val outputState = tx.outputsOfType<SomethingState>().single()
        val command = tx.commands.single()
        if (outputState.owner.owningKey !in command.signers) throw Exception( "Command lists output's two parties as signers")
    }

    class SomethingCommand: CommandData
}

/* Flow **/
@InitiatingFlow
@StartableByRPC
class SomethingFlow : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val outputState = SomethingState(ourIdentity)
        val outputStateAndContract = StateAndContract(outputState, SOMETHING_CONTRACT_ID)
        val command = Command(SomethingContract.SomethingCommand(), ourIdentity.owningKey)
        val txBuilder = TransactionBuilder(notary).withItems(outputStateAndContract, command)

        txBuilder.verify(serviceHub)

        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        subFlow(FinalityFlow(signedTx))
    }
}