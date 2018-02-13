package examples

import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.LedgerTransaction

class IAmAContract : Contract {
    companion object {
        // Used to reference the contract in transactions.
        val CONTRACT_ID = "examples.IAmAContract"
    }

    interface Commands: CommandData {
        // A command that is only used to parametrise contract verification.
        class TypeOnlyCommand: TypeOnlyCommandData(), Commands
        // A command that also contains data to be used during contract verification.
        class CommandWithData(val contents: String): Commands
    }

    override fun verify(tx: LedgerTransaction) = requireThat {
        // **************************************************
        // 1st type of checking - "shape" of the transaction:
        // **************************************************
        "There are five input states" using (tx.inputs.size == 5)
        "There are four output states" using (tx.inputs.size == 4)
        "There are three commands" using (tx.commands.size == 3)
        "There are two attachments" using (tx.attachments.size == 2)
        "There is a timestamp" using (tx.timeWindow != null)

        // ********************************************
        // 2nd type of checking - transaction contents:
        // ********************************************

        // INPUTS
        // Grabbing all input StateAndRefs.
        val inputStateAndRefs = tx.inputs
        // Grabbing all input states.
        val inputStates = tx.inputStates
        // Grabbing all input states of type IAmAlsoAState.
        val iAmAAlsoAStateInputs = tx.inputsOfType<IAmAlsoAState>()
        // Grabbing all input states of type IAmAlsoAState that meet a criterion.
        val filteredIAmAAlsoAStateInputs = tx.filterInputs<IAmAlsoAState> { it.data == "state data" }
        // Grabbing the single input state of type IAmAlsoAState that meets a criterion.
        val iAmAAlsoAStateInput = tx.findInput<IAmAlsoAState> { it.data == "state data" }

        // OUTPUTS
        // Grabbing all output TransactionStates.
        val outputTransactionStates = tx.outputs
        // Grabbing all output states.
        val outputStates = tx.outputStates
        // Grabbing all output states of type IAmAlsoAState.
        val iAmAAlsoAStateOutputs = tx.outputsOfType<IAmAlsoAState>()
        // Grabbing all output states of type IAmAlsoAState that meet a criterion.
        val filteredIAmAAlsoAStateOutputs = tx.filterOutputs<IAmAlsoAState> { it.data == "state data" }
        // Grabbing the single output state of type IAmAlsoAState that meets a criterion.
        val iAmAAlsoAStateOutput = tx.findOutput<IAmAlsoAState> { it.data == "state data" }

        // COMMANDS
        // Grabbing all commands.
        val commands = tx.commands
        // Grabbing all commands associated with this contract.
        val iAmAContractCommands = tx.commandsOfType<Commands>()
        // Grabbing a single command of type TypeOnlyCommand.
        val typeOnlyCommand = tx.commands.requireSingleCommand<Commands.TypeOnlyCommand>()
        // Grabbing all the commands of type CommandWithData that meet a criterion.
        val filteredCommandsWithData = tx.filterCommands<Commands.CommandWithData> { it.contents == "command contents" }
        // Grabbing the single command of type CommandWithData that meets a criterion.
        val commandWithData = tx.findCommand<Commands.CommandWithData> { it.contents == "command contents" }
        // Each command pairs a list of signers with a value. Here, we grab the command's value.
        val commandWithDataValue = commandWithData.value

        // ATTACHMENTS
        // Grabbing all attachments.
        val attachments = tx.attachments
        // Grabbing an attachment by hash.
        val attachment = tx.getAttachment(SecureHash.Companion.sha256("TEST_HASH"))

        // TIME WINDOWS
        val timewindow = tx.timeWindow

        // IMPOSING CONSTRAINTS
        "The input and output IAmAlsoAState have the same data" using
                (iAmAAlsoAStateInput.data == iAmAAlsoAStateOutput.data)
        "The CommandWithData's data matches the output IAmAlsoAState's data" using
                (commandWithDataValue.contents == iAmAAlsoAStateOutput.data)
        // And so on...

        // **********************************************
        // 3rd type of checking - transaction signatures:
        // **********************************************
        // Extracting the list of signers from a command.
        val commandWithDataSigners = commandWithData.signers
        // Checking that the input IAmAAlsoAState's party is a required signer.
        "The input IAmAAlsoAState's party is a required signer" using
                (commandWithDataSigners.contains(iAmAAlsoAStateInput.person.owningKey))
        // Checking that the output IAmAAlsoAState's party is a required signer.
        "The output IAmAAlsoAState's party is a required signer" using
                (commandWithDataSigners.contains(iAmAAlsoAStateOutput.person.owningKey))
        // And so on...
    }
}