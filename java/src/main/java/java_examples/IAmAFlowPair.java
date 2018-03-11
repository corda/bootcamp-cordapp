package java_examples;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.internal.FetchDataFlow;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.Vault.Page;
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.core.utilities.UntrustworthyData;
import org.jetbrains.annotations.NotNull;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.crypto.Crypto.generateKeyPair;

@SuppressWarnings("unused")
public class IAmAFlowPair {
    // ``InitiatorFlow`` is our first flow, and will communicate with
    // ``ResponderFlow``, below.
    // We mark ``InitiatorFlow`` as an ``InitiatingFlow``, allowing it to be
    // started directly by the node.
    @InitiatingFlow
    // We also mark ``InitiatorFlow`` as ``StartableByRPC``, allowing the
    // node's owner to start the flow via RPC.
    @StartableByRPC
    // Every flow must subclass ``FlowLogic``. The generic indicates the
    // flow's return type.
    public static class IAmAFlow extends FlowLogic<Void> {

        private final boolean arg1;
        private final int arg2;
        private final Party counterparty;
        private final Party regulator;

        public IAmAFlow(boolean arg1, int arg2, Party counterparty, Party regulator) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.counterparty = counterparty;
            this.regulator = regulator;
        }

        /*----------------------------------
         * WIRING UP THE PROGRESS TRACKER *
        ----------------------------------*/
        // Giving our flow a progress tracker allows us to see the flow's
        // progress visually in our node's CRaSH shell.
        private static final Step ID_OTHER_NODES = new Step("Identifying other nodes on the network.");
        private static final Step SENDING_AND_RECEIVING_DATA = new Step("Sending data between parties.");
        private static final Step EXTRACTING_VAULT_STATES = new Step("Extracting states from the vault.");
        private static final Step OTHER_TX_COMPONENTS = new Step("Gathering a transaction's other components.");
        private static final Step TX_BUILDING = new Step("Building a transaction.");
        private static final Step TX_SIGNING = new Step("Signing a transaction.");
        private static final Step TX_VERIFICATION = new Step("Verifying a transaction.");
        private static final Step SIGS_GATHERING = new Step("Gathering a transaction's signatures.") {
            // Wiring up a child progress tracker allows us to see the
            // subflow's progress steps in our flow's progress tracker.
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.tracker();
            }
        };
        private static final Step VERIFYING_SIGS = new Step("Verifying a transaction's signatures.");
        private static final Step FINALISATION = new Step("Finalising a transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                ID_OTHER_NODES,
                SENDING_AND_RECEIVING_DATA,
                EXTRACTING_VAULT_STATES,
                OTHER_TX_COMPONENTS,
                TX_BUILDING,
                TX_SIGNING,
                TX_VERIFICATION,
                SIGS_GATHERING,
                FINALISATION
        );

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // We'll be using a dummy public key for demonstration purposes.
            PublicKey dummyPubKey = generateKeyPair().getPublic();

            /*---------------------------
             * IDENTIFYING OTHER NODES *
            ---------------------------*/
            progressTracker.setCurrentStep(ID_OTHER_NODES);

            // A transaction generally needs a notary:
            //   - To prevent double-spends if the transaction has inputs
            //   - To serve as a timestamping authority if the transaction has a
            //     time-window
            // We retrieve a notary from the network map.
            CordaX500Name notaryName = new CordaX500Name("Notary Service", "London", "GB");
            Party specificNotary = getServiceHub().getNetworkMapCache().getNotary(notaryName);
            // Alternatively, we can pick an arbitrary notary from the notary
            // list. However, it is always preferable to specify the notary
            // explicitly, as the notary list might change when new notaries are
            // introduced, or old ones decommissioned.
            Party firstNotary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // We may also need to identify a specific counterparty. We do so
            // using the identity service.
            CordaX500Name counterPartyName = new CordaX500Name("NodeA", "London", "GB");
            Party namedCounterparty = getServiceHub().getIdentityService().wellKnownPartyFromX500Name(counterPartyName);
            Party keyedCounterparty = getServiceHub().getIdentityService().partyFromKey(dummyPubKey);

            /*------------------------------
             * SENDING AND RECEIVING DATA *
            ------------------------------*/
            progressTracker.setCurrentStep(SENDING_AND_RECEIVING_DATA);

            // We start by initiating a flow session with the counterparty. We
            // will use this session to send and receive messages from the
            // counterparty.
            FlowSession counterpartySession = initiateFlow(counterparty);

            // We can send arbitrary data to a counterparty.
            // If this is the first ``send``, the counterparty will either:
            // 1. Ignore the message if they are not registered to respond
            //    to messages from this flow.
            // 2. Start the flow they have registered to respond to this flow,
            //    and run the flow until the first call to ``receive``, at
            //    which point they process the message.
            // In other words, we are assuming that the counterparty is
            // registered to respond to this flow, and has a corresponding
            // ``receive`` call.
            counterpartySession.send(new Object());

            // We can wait to receive arbitrary data of a specific type from a
            // counterparty. Again, this implies a corresponding ``send`` call
            // in the counterparty's flow. A few scenarios:
            // - We never receive a message back. In the current design, the
            //   flow is paused until the node's owner kills the flow.
            // - Instead of sending a message back, the counterparty throws a
            //   ``FlowException``. This exception is propagated back to us,
            //   and we can use the error message to establish what happened.
            // - We receive a message back, but it's of the wrong type. In
            //   this case, a ``FlowException`` is thrown.
            // - We receive back a message of the correct type. All is good.
            //
            // Upon calling ``receive()`` (or ``sendAndReceive()``), the
            // ``FlowLogic`` is suspended until it receives a response.
            //
            // We receive the data wrapped in an ``UntrustworthyData``
            // instance. This is a reminder that the data we receive may not
            // be what it appears to be! We must unwrap the
            // ``UntrustworthyData`` using a lambda.
            UntrustworthyData<Integer> packet1 = counterpartySession.receive(Integer.class);
            Integer integer = packet1.unwrap(data -> {
                // Perform checking on the object received.
                // T O D O: Check the received object.
                // Return the object.
                return data;
            });

            // We can also use a single call to send data to a counterparty
            // and wait to receive data of a specific type back. The type of
            // data sent doesn't need to match the type of the data received
            // back.
            UntrustworthyData<Boolean> packet2 = counterpartySession.sendAndReceive(Boolean.class, "You can send and receive any class!");
            Boolean bool = packet2.unwrap(data -> {
                // Perform checking on the object received.
                // T O D O: Check the received object.
                // Return the object.
                return data;
            });

            // We're not limited to sending to and receiving from a single
            // counterparty. A flow can send messages to as many parties as it
            // likes, and each party can invoke a different response flow.
            FlowSession regulatorSession = initiateFlow(regulator);
            regulatorSession.send(new Object());
            UntrustworthyData<Object> packet3 = regulatorSession.receive(Object.class);

            /*------------------------------------
             * EXTRACTING STATES FROM THE VAULT *
            ------------------------------------*/
            progressTracker.setCurrentStep(EXTRACTING_VAULT_STATES);

            // Let's assume there are already some ``IAmAlsoAState``s in our
            // node's vault, stored there as a result of running past flows,
            // and we want to consume them in a transaction. There are many
            // ways to extract these states from our vault.

            // For example, we would extract any unconsumed ``IAmAlsoAState``s
            // from our vault as follows:
            VaultQueryCriteria criteria = new VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
            Page<IAmAlsoAState> results = getServiceHub().getVaultService().queryBy(IAmAlsoAState.class, criteria);
            List<StateAndRef<IAmAlsoAState>> dummyStates = results.getStates();

            // For a full list of the available ways of extracting states from
            // the vault, see the Vault Query docs page.

            // When building a transaction, input states are passed in as
            // ``StateRef`` instances, which pair the hash of the transaction
            // that generated the state with the state's index in the outputs
            // of that transaction. In practice, we'd pass the transaction hash
            // or the ``StateRef`` as a parameter to the flow, or extract the
            // ``StateRef`` from our vault.
            StateRef ourStateRef = new StateRef(SecureHash.sha256("DummyTransactionHash"), 0);
            // A ``StateAndRef`` pairs a ``StateRef`` with the state it points to.
            StateAndRef ourStateAndRef = getServiceHub().toStateAndRef(ourStateRef);

            /*------------------------------------------
             * GATHERING OTHER TRANSACTION COMPONENTS *
            ------------------------------------------*/
            progressTracker.setCurrentStep(OTHER_TX_COMPONENTS);

            // Output states are constructed from scratch.
            IAmAlsoAState ourOutputState = new IAmAlsoAState("data", counterparty, new UniqueIdentifier());
            // Or as copies of other states with some properties changed.
            IAmAlsoAState ourOtherOutputState = new IAmAlsoAState("new data", ourOutputState.getPerson(), ourOutputState.getLinearId());

            // We then need to pair our output state with a contract.
            StateAndContract ourOutput = new StateAndContract(ourOutputState, IAmAContract.CONTRACT_ID);

            // Commands pair a ``CommandData`` instance with a list of
            // public keys. To be valid, the transaction requires a signature
            // matching every public key in all of the transaction's commands.
            IAmAContract.Commands.TypeOnlyCommand commandData = new IAmAContract.Commands.TypeOnlyCommand();
            PublicKey ourPubKey = getServiceHub().getMyInfo().getLegalIdentitiesAndCerts().get(0).getOwningKey();
            PublicKey counterpartyPubKey = counterparty.getOwningKey();
            List<PublicKey> requiredSigners = ImmutableList.of(ourPubKey, counterpartyPubKey);
            Command<IAmAContract.Commands.TypeOnlyCommand> ourCommand = new Command<>(commandData, requiredSigners);

            // ``CommandData`` can either be:
            // 1. Of type ``TypeOnlyCommandData``, in which case it only
            //    serves to attach signers to the transaction and possibly
            //    fork the contract's verification logic.
            TypeOnlyCommandData typeOnlyCommandData = new IAmAContract.Commands.TypeOnlyCommand();
            // 2. Include additional data which can be used by the contract
            //    during verification, alongside fulfilling the roles above
            CommandData commandDataWithData = new IAmAContract.Commands.CommandWithData("new data");

            // Attachments are identified by their hash.
            // The attachment with the corresponding hash must have been
            // uploaded ahead of time via the node's RPC interface.
            SecureHash ourAttachment = SecureHash.sha256("DummyAttachment");

            // Time windows represent the period of time during which a
            // transaction must be notarised. They can have a start and an end
            // time, or be open at either end.
            TimeWindow ourTimeWindow = TimeWindow.between(Instant.MIN, Instant.MAX);
            TimeWindow ourAfter = TimeWindow.fromOnly(Instant.MIN);
            TimeWindow ourBefore = TimeWindow.untilOnly(Instant.MAX);

            // We can also define a time window as an ``Instant`` +/- a time
            // tolerance (e.g. 30 seconds):
            TimeWindow ourTimeWindow2 = TimeWindow.withTolerance(getServiceHub().getClock().instant(), Duration.ofSeconds(30));
            // Or as a start-time plus a duration:
            TimeWindow ourTimeWindow3 = TimeWindow.fromStartAndDuration(getServiceHub().getClock().instant(), Duration.ofSeconds(30));

            /*------------------------
             * TRANSACTION BUILDING *
            ------------------------*/
            progressTracker.setCurrentStep(TX_BUILDING);

            // If our transaction has input states or a time-window, we must instantiate it with a
            // notary.
            TransactionBuilder txBuilder = new TransactionBuilder(specificNotary);

            // Otherwise, we can choose to instantiate it without one:
            TransactionBuilder txBuilderNoNotary = new TransactionBuilder();

            // We add items to the transaction builder using ``TransactionBuilder.withItems``:
            txBuilder.withItems(
                    // Inputs, as ``StateAndRef``s that reference to the outputs of previous transactions
                    ourStateAndRef,
                    // Outputs, as ``StateAndContract``s
                    ourOutput,
                    // Commands, as ``Command``s
                    ourCommand,
                    // Attachments, as ``SecureHash``es
                    ourAttachment,
                    // A time-window, as ``TimeWindow``
                    ourTimeWindow
            );

            // We can also add items using methods for the individual components.

            // The individual methods for adding input states and attachments:
            txBuilder.addInputState(ourStateAndRef);
            txBuilder.addAttachment(ourAttachment);

            // An output state can be added as a ``ContractState``, contract class name and notary.
            txBuilder.addOutputState(ourOutputState, IAmAContract.CONTRACT_ID, specificNotary);
            // We can also leave the notary field blank, in which case the transaction's default
            // notary is used.
            txBuilder.addOutputState(ourOutputState, IAmAContract.CONTRACT_ID);
            // Or we can add the output state as a ``TransactionState``, which already specifies
            // the output's contract and notary.
            TransactionState txState = new TransactionState<>(ourOutputState, IAmAContract.CONTRACT_ID, specificNotary);

            // Commands can be added as ``Command``s.
            txBuilder.addCommand(ourCommand);
            // Or as ``CommandData`` and a ``vararg PublicKey``.
            txBuilder.addCommand(commandData, ourPubKey, counterpartyPubKey);

            // We can set a time-window directly.
            txBuilder.setTimeWindow(ourTimeWindow);
            // Or as a start time plus a duration (e.g. 45 seconds).
            txBuilder.setTimeWindow(getServiceHub().getClock().instant(), Duration.ofSeconds(45));

            /*-----------------------
             * TRANSACTION SIGNING *
            -----------------------*/
            progressTracker.setCurrentStep(TX_SIGNING);

            // We finalise the transaction by signing it,
            // converting it into a ``SignedTransaction``.
            SignedTransaction onceSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            // We can also sign the transaction using a different public key:
            PartyAndCertificate otherIdentity = getServiceHub().getKeyManagementService().freshKeyAndCert(getOurIdentityAndCert(), false);
            SignedTransaction onceSignedTx2 = getServiceHub().signInitialTransaction(txBuilder, otherIdentity.getOwningKey());

            // If instead this was a ``SignedTransaction`` that we'd received
            // from a counterparty and we needed to sign it, we would add our
            // signature using:
            SignedTransaction twiceSignedTx = getServiceHub().addSignature(onceSignedTx);
            // Or, if we wanted to use a different public key:
            PartyAndCertificate otherIdentity2 = getServiceHub().getKeyManagementService().freshKeyAndCert(getOurIdentityAndCert(), false);
            SignedTransaction twiceSignedTx2 = getServiceHub().addSignature(onceSignedTx, otherIdentity2.getOwningKey());

            // We can also generate a signature over the transaction without
            // adding it to the transaction itself. We may do this when
            // sending just the signature in a flow instead of returning the
            // entire transaction with our signature. This way, the receiving
            // node does not need to check we haven't changed anything in the
            // transaction.
            TransactionSignature sig = getServiceHub().createSignature(onceSignedTx);
            // And again, if we wanted to use a different public key:
            TransactionSignature sig2 = getServiceHub().createSignature(onceSignedTx, otherIdentity2.getOwningKey());

            /*----------------------------
             * TRANSACTION VERIFICATION *
            ----------------------------*/
            progressTracker.setCurrentStep(TX_VERIFICATION);

            // Verifying a transaction will also verify every transaction in
            // the transaction's dependency chain, which will require
            // transaction data access on counterparty's node. The
            // ``SendTransactionFlow`` can be used to automate the sending and
            // data vending process. The ``SendTransactionFlow`` will listen
            // for data request until the transaction is resolved and verified
            // on the other side:
            subFlow(new SendTransactionFlow(counterpartySession, twiceSignedTx));

            // Optional request verification to further restrict data access.
            subFlow(new SendTransactionFlow(counterpartySession, twiceSignedTx) {
                @Override
                protected void verifyDataRequest(@NotNull FetchDataFlow.Request.Data dataRequest) {
                    // Extra request verification.
                }
            });

            // We can receive the transaction using ``ReceiveTransactionFlow``,
            // which will automatically download all the dependencies and verify
            // the transaction and then record in our vault
            SignedTransaction verifiedTransaction = subFlow(new ReceiveTransactionFlow(counterpartySession));

            // We can also send and receive a `StateAndRef` dependency chain and automatically resolve its dependencies.
            subFlow(new SendStateAndRefFlow(counterpartySession, dummyStates));

            // On the receive side ...
            List<StateAndRef<IAmAlsoAState>> resolvedStateAndRef = subFlow(new ReceiveStateAndRefFlow<IAmAlsoAState>(counterpartySession));

            try {

                // We can now verify the transaction to ensure that it satisfies
                // the contracts of all the transaction's input and output states.
                twiceSignedTx.verify(getServiceHub());

                // We'll often want to perform our own additional verification
                // too. Just because a transaction is valid based on the contract
                // rules and requires our signature doesn't mean we have to
                // sign it! We need to make sure the transaction represents an
                // agreement we actually want to enter into.

                // To do this, we need to convert our ``SignedTransaction``
                // into a ``LedgerTransaction``. This will use our ServiceHub
                // to resolve the transaction's inputs and attachments into
                // actual objects, rather than just references.
                LedgerTransaction ledgerTx = twiceSignedTx.toLedgerTransaction(getServiceHub());

                // We can now perform our additional verification.
                IAmAlsoAState outputState = ledgerTx.outputsOfType(IAmAlsoAState.class).get(0);
                if (!outputState.getData().equals("new data")) {
                    // ``FlowException`` is a special exception type. It will be
                    // propagated back to any counterparty flows waiting for a
                    // message from this flow, notifying them that the flow has
                    // failed.
                    throw new FlowException("We expected different data.");
                }

            } catch (GeneralSecurityException e) {
                // Handle this as required.
            }

            // Of course, if you are not a required signer on the transaction,
            // you have no power to decide whether it is valid or not. If it
            // collects signatures from all the required signers and is
            // contractually valid, it's a valid ledger update.

            /*------------------------
             * GATHERING SIGNATURES *
            ------------------------*/
            progressTracker.setCurrentStep(SIGS_GATHERING);

            // The list of parties who need to sign a transaction is dictated
            // by the transaction's commands. Once we've signed a transaction
            // ourselves, we can automatically gather the signatures of the
            // other required signers using ``CollectSignaturesFlow``.
            // The responder flow will need to call ``SignTransactionFlow``.
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(twiceSignedTx, Collections.emptySet(), SIGS_GATHERING.childProgressTracker()));

            /*------------------------
             * VERIFYING SIGNATURES *
            ------------------------*/
            progressTracker.setCurrentStep(VERIFYING_SIGS);

            try {

                // We can verify that a transaction has all the required
                // signatures, and that they're all valid, by running:
                fullySignedTx.verifyRequiredSignatures();

                // If the transaction is only partially signed, we have to pass in
                // a list of the public keys corresponding to the missing
                // signatures, explicitly telling the system not to check them.
                onceSignedTx.verifySignaturesExcept(counterpartyPubKey);

                // We can also choose to only check the signatures that are
                // present. BE VERY CAREFUL - this function provides no guarantees
                // that the signatures are correct, or that none are missing.
                twiceSignedTx.checkSignaturesAreValid();
            } catch (GeneralSecurityException e) {
                // Handle this as required.
            }

            /*------------------------------
             * FINALISING THE TRANSACTION *
            ------------------------------*/
            progressTracker.setCurrentStep(FINALISATION);

            // We notarise the transaction and get it recorded in the vault of
            // the participants of all the transaction's states.
            SignedTransaction notarisedTx1 = subFlow(new FinalityFlow(fullySignedTx, FINALISATION.childProgressTracker()));
            // We can also choose to send it to additional parties who aren't one
            // of the state's participants.
            Set<Party> additionalParties = Collections.singleton(regulator);
            SignedTransaction notarisedTx2 = subFlow(new FinalityFlow(fullySignedTx, additionalParties, FINALISATION.childProgressTracker()));

            return null;
        }
    }

    // ``ResponderFlow`` is our second flow, and will communicate with
    // ``InitiatorFlow``.
    // We mark ``ResponderFlow`` as an ``InitiatedByFlow``, meaning that it
    // can only be started in response to a message from its initiating flow.
    // That's ``InitiatorFlow`` in this case.
    // Each node also has several flow pairs registered by default - see
    // ``AbstractNode.installCoreFlows``.
    @InitiatedBy(IAmAFlow.class)
    public static class IAmAFlowResponder extends FlowLogic<Void> {

        private final FlowSession counterpartySession;

        public IAmAFlowResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        private static final Step RECEIVING_AND_SENDING_DATA = new Step("Sending data between parties.");
        private static final Step SIGNING = new Step("Responding to CollectSignaturesFlow.");
        private static final Step FINALISATION = new Step("Finalising a transaction.");

        private final ProgressTracker progressTracker = new ProgressTracker(
                RECEIVING_AND_SENDING_DATA,
                SIGNING,
                FINALISATION
        );

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // The ``ResponderFlow` has all the same APIs available. It looks
            // up network information, sends and receives data, and constructs
            // transactions in exactly the same way.

            /*------------------------------
             * SENDING AND RECEIVING DATA *
             -----------------------------*/
            progressTracker.setCurrentStep(RECEIVING_AND_SENDING_DATA);

            // We need to respond to the messages sent by the initiator:
            // 1. They sent us an ``Object`` instance
            // 2. They waited to receive an ``Integer`` instance back
            // 3. They sent a ``String`` instance and waited to receive a
            //    ``Boolean`` instance back
            // Our side of the flow must mirror these calls.
            Object obj = counterpartySession.receive(Object.class).unwrap(data -> data);
            String string = counterpartySession.sendAndReceive(String.class, 99).unwrap(data -> data);
            counterpartySession.send(true);

            /*-----------------------------------------
             * RESPONDING TO COLLECT_SIGNATURES_FLOW *
            -----------------------------------------*/
            progressTracker.setCurrentStep(SIGNING);

            // The responder will often need to respond to a call to
            // ``CollectSignaturesFlow``. It does so my invoking its own
            // ``SignTransactionFlow`` subclass.
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherSession, ProgressTracker progressTracker) {
                    super(otherSession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        // Any additional checking we see fit...
                        IAmAlsoAState outputState = (IAmAlsoAState) stx.getTx().getOutputs().get(0).getData();
                        assert (outputState.getData().equals("new data"));
                        return null;
                    });
                }
            }

            subFlow(new SignTxFlow(counterpartySession, SignTransactionFlow.tracker()));

            /*------------------------------
             * FINALISING THE TRANSACTION *
            ------------------------------*/
            progressTracker.setCurrentStep(FINALISATION);

            // Nothing to do here! As long as some other party calls
            // ``FinalityFlow``, the recording of the transaction on our node
            // we be handled automatically.

            return null;
        }
    }
}