package java_examples;

import net.corda.core.contracts.*;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;

// Like all contracts, implements `Contract`.
public class ArtContract implements Contract {
    // Used to reference the contract in transactions.
    public static final String ID = "java_examples.ArtContract";

    public interface Commands extends CommandData {
        class Issue implements Commands { }
        class Transfer implements Commands { }
        class Exit implements Commands { }
    }

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);

        if (command.getValue() instanceof Commands.Issue) {
            // Issue transaction rules...

        } else if (command.getValue() instanceof Commands.Transfer) {
            // Checking the shape of the transaction.
            if (tx.getInputStates().size() != 1) throw new IllegalArgumentException("Art transfer should have one inputs.");
            if (tx.getOutputStates().size() != 1) throw new IllegalArgumentException("Art transfer should have one output.");
            if (tx.inputsOfType(ArtState.class).size() != 1) throw new IllegalArgumentException("Art transfer input should be an ArtState.");
            if (tx.outputsOfType(ArtState.class).size() != 1) throw new IllegalArgumentException("Art transfer output should be an ArtState.");

            // Grabbing the transaction's contents.
            final ArtState artStateInput = tx.inputsOfType(ArtState.class).get(0);
            final ArtState artStateOutput = tx.outputsOfType(ArtState.class).get(0);

            // Checking the transaction's contents.
            if (!(artStateInput.getArtist().equals(artStateOutput.getArtist())))
                throw new IllegalArgumentException("Art transfer input and output should have the same artist.");
            if (!(artStateInput.getTitle().equals(artStateOutput.getTitle())))
                throw new IllegalArgumentException("Art transfer input and output should have the same title.");
            if (!(artStateInput.getAppraiser().equals(artStateOutput.getAppraiser())))
                throw new IllegalArgumentException("Art transfer input and output should have the same appraiser.");
            if (artStateInput.getOwner().equals(artStateOutput.getOwner()))
                throw new IllegalArgumentException("Art transfer input and output should have different owners.");

            // Checking the transaction's required signers.
            final List<PublicKey> requiredSigners = command.getSigners();
            if (!(requiredSigners.contains(artStateInput.getOwner().getOwningKey())))
                throw new IllegalArgumentException("Art transfer should have input's owner as a required signer.");
            if (!(requiredSigners.contains(artStateOutput.getOwner().getOwningKey())))
                throw new IllegalArgumentException("Art transfer should have output's owner as a required signer.");

        } else if (command.getValue() instanceof Commands.Exit) {
            // Exit transaction rules...

        } else throw new IllegalArgumentException("Unrecognised command.");
    }
}