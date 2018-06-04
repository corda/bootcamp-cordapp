package java_examples;

import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IAmAContract implements Contract {
    // Used to reference the contract in transactions.
    public static final String CONTRACT_ID = "java_examples.IAmAContract";

    public interface Commands extends CommandData {
        // A command that is only used to parametrise contract verification.
        class TypeOnlyCommand extends TypeOnlyCommandData implements Commands {
        }

        // A command that also contains data to be used during contract verification.
        class CommandWithData implements Commands {
            private final String contents;

            public CommandWithData(String contents) {
                this.contents = contents;
            }

            public String getContents() {
                return contents;
            }
        }
    }

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        requireThat(require -> {
            // **************************************************
            // 1st type of checking - "shape" of the transaction:
            // **************************************************
            require.using("There are five input states", tx.getInputs().size() == 5);
            require.using("There are four output states", tx.getOutputs().size() == 4);
            require.using("There are three commands", tx.getCommands().size() == 3);
            require.using("There are two attachments", tx.getAttachments().size() == 2);
            require.using("There is a timestamp", tx.getTimeWindow() != null);

            // ********************************************
            // 2nd type of checking - transaction contents:
            // ********************************************

            // INPUTS
            // Grabbing all input StateAndRefs.
            final List<StateAndRef<ContractState>> inputStateAndRefs = tx.getInputs();
            // Grabbing all input states.
            final List<ContractState> inputStates = tx.getInputStates();
            // Grabbing all input states of type IAmAlsoAState.
            final List<IAmAlsoAState> iAmAAlsoAStateInputs = tx.inputsOfType(IAmAlsoAState.class);
            // Grabbing all input states of type IAmAlsoAState that meet a criterion.
            final List<IAmAlsoAState> filteredIAmAAlsoAStateInputs = tx.filterInputs(IAmAlsoAState.class, state -> state.getData().equals("state data"));
            // Grabbing the single input state of type IAmAlsoAState that meets a criterion.
            final IAmAlsoAState iAmAAlsoAStateInput = tx.findInput(IAmAlsoAState.class, state -> state.getData().equals("state data"));

            // OUTPUTS
            // Grabbing all output TransactionStates.
            final List<TransactionState<ContractState>> outputTransactionStates = tx.getOutputs();
            // Grabbing all output states.
            final List<ContractState> outputStates = tx.getOutputStates();
            // Grabbing all output states of type IAmAlsoAState.
            final List<IAmAlsoAState> iAmAAlsoAStateOutputs = tx.outputsOfType(IAmAlsoAState.class);
            // Grabbing all output states of type IAmAlsoAState that meet a criterion.
            final List<IAmAlsoAState> filteredIAmAAlsoAStateOutputs = tx.filterOutputs(IAmAlsoAState.class, state -> state.getData().equals("state data"));
            // Grabbing the single output state of type IAmAlsoAState that meets a criterion.
            final IAmAlsoAState iAmAAlsoAStateOutput = tx.findOutput(IAmAlsoAState.class, state -> state.getData().equals("state data"));

            // COMMANDS
            // Grabbing all commands.
            final List<CommandWithParties<CommandData>> commands = tx.getCommands();
            // Grabbing all commands associated with this contract.
            final List<Command<Commands>> iAmAContractCommands = tx.commandsOfType(Commands.class);
            // Grabbing a single command of type TypeOnlyCommand.
            final CommandWithParties<Commands.TypeOnlyCommand> typeOnlyCommand = requireSingleCommand(tx.getCommands(), Commands.TypeOnlyCommand.class);
            // Grabbing all the commands of type CommandWithData that meet a criterion.
            final List<Command<Commands.CommandWithData>> filteredCommandsWithData = tx.filterCommands(Commands.CommandWithData.class, command -> command.contents.equals("command contents"));
            // Grabbing the single command of type CommandWithData that meets a criterion.
            final Command<Commands.CommandWithData> commandWithData = tx.findCommand(Commands.CommandWithData.class, command -> command.contents.equals("command contents"));
            // Each command pairs a list of signers with a value. Here, we grab the command's value.
            final Commands.CommandWithData commandWithDataValue = commandWithData.getValue();

            // ATTACHMENTS
            // Grabbing all attachments.
            final List<Attachment> attachments = tx.getAttachments();
            // Grabbing an attachment by hash.
            final Attachment attachment = tx.getAttachment(SecureHash.sha256("TEST_HASH"));

            // TIME WINDOWS
            final TimeWindow timewindow = tx.getTimeWindow();

            // IMPOSING CONSTRAINTS
            require.using("The input and output IAmAlsoAState have the same data",
                    iAmAAlsoAStateInput.getData().equals(iAmAAlsoAStateOutput.getData()));
            require.using("The CommandWithData's data matches the output IAmAlsoAState's data",
                    commandWithDataValue.getContents().equals(iAmAAlsoAStateOutput.getData()));
            // And so on...

            // **********************************************
            // 3rd type of checking - transaction signatures:
            // **********************************************
            // Extracting the list of signers from a command.
            final List<PublicKey> commandWithDataSigners = commandWithData.getSigners();
            // Checking that the input IAmAAlsoAState's party is a required signer.
            require.using("The input IAmAAlsoAState's party is a required signer",
                    commandWithDataSigners.contains(iAmAAlsoAStateInput.getPerson().getOwningKey()));
            // Checking that the output IAmAAlsoAState's party is a required signer.
            require.using("The output IAmAAlsoAState's party is a required signer",
                    commandWithDataSigners.contains(iAmAAlsoAStateOutput.getPerson().getOwningKey()));
            // And so on...

            return null;
        });
    }
}