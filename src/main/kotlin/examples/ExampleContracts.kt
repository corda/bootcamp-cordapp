package examples

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class IAmAContract : Contract {
    companion object {
        // Used to reference the contract in transactions.
        val CONTRACT_ID = "examples.IAmAContract"
    }

    interface Commands: CommandData {
        // A command that is only used to parameterise contract verification.
        class TypeOnlyCommand: TypeOnlyCommandData(), Commands
        // A command that also contains data to be used during contract verification.
        class CommandWithData(val contents: String): Commands
    }

    override fun verify(tx: LedgerTransaction) = requireThat {
        // 1ST TYPE OF CHECKING - "SHAPE" OF THE TRANSACTION:
        "There are five input states" using (tx.inputs.size == 5)
        "There are four output states" using (tx.inputs.size == 4)
        "There are three commands" using (tx.commands.size == 3)
        "There are two attachments" using (tx.attachments.size == 2)
        "There is a timestamp" using (tx.timeWindow != null)

        // 2ND TYPE OF CHECKING - TRANSACTION CONTENTS:
        // TODO:

        // 3RD TYPE OF CHECKING - TRANSACTION SIGNATURES:
        // TODO:
    }
}