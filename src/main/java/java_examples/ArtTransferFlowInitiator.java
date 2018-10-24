package java_examples;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.find;

// `InitiatingFlow` means that we can start the flow directly (instead of
// solely in response to another flow).
@InitiatingFlow
// `StartableByRPC` means that a node operator can start the flow via RPC.
@StartableByRPC
// Like all states, implements `FlowLogic`.
public class ArtTransferFlowInitiator extends FlowLogic<Void> {
    private final String title;
    private final String artist;
    private final Party newOwner;

    // Flows can take constructor arguments to parameterize the execution of the flow.
    public ArtTransferFlowInitiator(String title, String artist, Party newOwner) {
        this.title = title;
        this.artist = artist;
        this.newOwner = newOwner;
    }

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    // Must be marked `@Suspendable` to allow the flow to be suspended
    // mid-execution.
    @Suspendable
    @Override
    // Overrides `call`, where we define the logic executed by the flow.
    public Void call() throws FlowException {
        // We extract all the `ArtState`s from the vault.
        List<StateAndRef<ArtState>> artStateAndRefs = getServiceHub().getVaultService().queryBy(ArtState.class).getStates();

        // We find the `ArtState` with the correct artist and title.
        StateAndRef<ArtState> inputArtStateAndRef = artStateAndRefs
                .stream().filter(artStateAndRef -> {
                    ArtState artState = artStateAndRef.getState().getData();
                    return artState.getArtist().equals(artist) && artState.getTitle().equals(title);
                }).findAny().orElseThrow(() -> new IllegalArgumentException("The piece of art was not found."));
        ArtState inputArtState = inputArtStateAndRef.getState().getData();

        // We throw an exception if the flow was not started by the art's current owner.
        if (!(getOurIdentity().equals(inputArtState.getOwner())))
            throw new IllegalStateException("This flow must be started by the current owner.");

        // We use the notary used by the input state.
        Party notary = inputArtStateAndRef.getState().getNotary();

        // We build a transaction using a `TransactionBuilder`.
        TransactionBuilder txBuilder = new TransactionBuilder();

        // After creating the `TransactionBuilder`, we must specify which
        // notary it will use.
        txBuilder.setNotary(notary);

        // We add the input ArtState to the transaction.
        txBuilder.addInputState(inputArtStateAndRef);

        // We add the output ArtState to the transaction. Note that we also
        // specify which contract class to use for verification.
        ArtState outputArtState = new ArtState(
                inputArtState.getArtist(),
                inputArtState.getTitle(),
                inputArtState.getAppraiser(),
                newOwner);
        txBuilder.addOutputState(outputArtState, ArtContract.ID);

        // We add the Issue command to the transaction.
        // Note that we also specific who is required to sign the transaction.
        ArtContract.Commands.Transfer commandData = new ArtContract.Commands.Transfer();
        List<PublicKey> requiredSigners = ImmutableList.of(
                inputArtState.getOwner().getOwningKey(), newOwner.getOwningKey());
        txBuilder.addCommand(commandData, requiredSigners);

        // We check that the transaction builder we've created meets the
        // contracts of the input and output states.
        txBuilder.verify(getServiceHub());

        // We finalise the transaction builder by signing it,
        // converting it into a `SignedTransaction`.
        SignedTransaction partlySignedTx = getServiceHub().signInitialTransaction(txBuilder);

        // We use `CollectSignaturesFlow` to automatically gather a
        // signature from each counterparty. The counterparty will need to
        // call `SignTransactionFlow` to decided whether or not to sign.
        FlowSession ownerSession = initiateFlow(newOwner);
        SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partlySignedTx, ImmutableSet.of(ownerSession)));

        // We use `FinalityFlow` to automatically notarise the transaction
        // and have it recorded by all the `participants` of all the
        // transaction's states.
        subFlow(new FinalityFlow(fullySignedTx));

        return null;
    }
}