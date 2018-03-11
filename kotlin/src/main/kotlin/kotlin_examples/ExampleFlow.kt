package kotlin_examples

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.crypto.generateKeyPair
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.identity.PartyAndCertificate
import net.corda.core.internal.FetchDataFlow
import net.corda.core.node.services.Vault.Page
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.seconds
import net.corda.core.utilities.unwrap
import java.security.PublicKey
import java.time.Instant

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
class IAmAFlow(val arg1: Boolean, val arg2: Int, private val counterparty: Party, val regulator: Party) : FlowLogic<Unit>() {
    /**---------------------------------
     * WIRING UP THE PROGRESS TRACKER *
    ---------------------------------**/
    // Giving our flow a progress tracker allows us to see the flow's
    // progress visually in our node's CRaSH shell.
    companion object {
        object ID_OTHER_NODES : Step("Identifying other nodes on the network.")
        object SENDING_AND_RECEIVING_DATA : Step("Sending data between parties.")
        object EXTRACTING_VAULT_STATES : Step("Extracting states from the vault.")
        object OTHER_TX_COMPONENTS : Step("Gathering a transaction's other components.")
        object TX_BUILDING : Step("Building a transaction.")
        object TX_SIGNING : Step("Signing a transaction.")
        object TX_VERIFICATION : Step("Verifying a transaction.")
        object SIGS_GATHERING : Step("Gathering a transaction's signatures.") {
            // Wiring up a child progress tracker allows us to see the
            // subflow's progress steps in our flow's progress tracker.
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object VERIFYING_SIGS : Step("Verifying a transaction's signatures.")
        object FINALISATION : Step("Finalising a transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                ID_OTHER_NODES,
                SENDING_AND_RECEIVING_DATA,
                EXTRACTING_VAULT_STATES,
                OTHER_TX_COMPONENTS,
                TX_BUILDING,
                TX_SIGNING,
                TX_VERIFICATION,
                SIGS_GATHERING,
                VERIFYING_SIGS,
                FINALISATION
        )
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suppress("RemoveExplicitTypeArguments")
    @Suspendable
    override fun call() {
        // We'll be using a dummy public key for demonstration purposes.
        val dummyPubKey: PublicKey = generateKeyPair().public

        /**--------------------------
         * IDENTIFYING OTHER NODES *
        --------------------------**/
        progressTracker.currentStep = ID_OTHER_NODES

        // A transaction generally needs a notary:
        //   - To prevent double-spends if the transaction has inputs
        //   - To serve as a timestamping authority if the transaction has a
        //     time-window
        // We retrieve the notary from the network map.
        val notaryName: CordaX500Name = CordaX500Name(
                organisation = "Notary Service",
                locality = "London",
                country = "GB")
        val specificNotary: Party = serviceHub.networkMapCache.getNotary(notaryName)!!
        // Alternatively, we can pick an arbitrary notary from the notary
        // list. However, it is always preferable to specify the notary
        // explicitly, as the notary list might change when new notaries are
        // introduced, or old ones decommissioned.
        val firstNotary: Party = serviceHub.networkMapCache.notaryIdentities.first()

        // We may also need to identify a specific counterparty. We do so
        // using the identity service.
        val counterpartyName: CordaX500Name = CordaX500Name(
                organisation = "NodeA",
                locality = "London",
                country = "GB")
        val namedCounterparty: Party = serviceHub.identityService.wellKnownPartyFromX500Name(counterpartyName) ?:
        throw IllegalArgumentException("Couldn't find counterparty for NodeA in identity service")
        val keyedCounterparty: Party = serviceHub.identityService.partyFromKey(dummyPubKey) ?:
        throw IllegalArgumentException("Couldn't find counterparty with key: $dummyPubKey in identity service")

        /**-----------------------------
         * SENDING AND RECEIVING DATA *
        -----------------------------**/
        progressTracker.currentStep = SENDING_AND_RECEIVING_DATA

        // We start by initiating a flow session with the counterparty. We
        // will use this session to send and receive messages from the
        // counterparty.
        val counterpartySession: FlowSession = initiateFlow(counterparty)

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
        counterpartySession.send(Any())

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
        val packet1: UntrustworthyData<Int> = counterpartySession.receive<Int>()
        val int: Int = packet1.unwrap { data ->
            // Perform checking on the object received.
            // T O D O: Check the received object.
            // Return the object.
            data
        }

        // We can also use a single call to send data to a counterparty
        // and wait to receive data of a specific type back. The type of
        // data sent doesn't need to match the type of the data received
        // back.
        val packet2: UntrustworthyData<Boolean> = counterpartySession.sendAndReceive<Boolean>("You can send and receive any class!")
        val boolean: Boolean = packet2.unwrap { data ->
            // Perform checking on the object received.
            // T O D O: Check the received object.
            // Return the object.
            data
        }

        // We're not limited to sending to and receiving from a single
        // counterparty. A flow can send messages to as many parties as it
        // likes, and each party can invoke a different response flow.
        val regulatorSession: FlowSession = initiateFlow(regulator)
        regulatorSession.send(Any())
        val packet3: UntrustworthyData<Any> = regulatorSession.receive<Any>()

        /**-----------------------------------
         * EXTRACTING STATES FROM THE VAULT *
        -----------------------------------**/
        progressTracker.currentStep = EXTRACTING_VAULT_STATES

        // Let's assume there are already some ``IAmAlsoAState``s in our
        // node's vault, stored there as a result of running past flows,
        // and we want to consume them in a transaction. There are many
        // ways to extract these states from our vault.

        // For example, we would extract any unconsumed ``IAmAlsoAState``s
        // from our vault as follows:
        val criteria: VaultQueryCriteria = VaultQueryCriteria() // default is UNCONSUMED
        val results: Page<IAmAlsoAState> = serviceHub.vaultService.queryBy<IAmAlsoAState>(criteria)
        val dummyStates: List<StateAndRef<IAmAlsoAState>> = results.states

        // For a full list of the available ways of extracting states from
        // the vault, see the Vault Query docs page.

        // When building a transaction, input states are passed in as
        // ``StateRef`` instances, which pair the hash of the transaction
        // that generated the state with the state's index in the outputs
        // of that transaction. In practice, we'd pass the transaction hash
        // or the ``StateRef`` as a parameter to the flow, or extract the
        // ``StateRef`` from our vault.
        val ourStateRef: StateRef = StateRef(SecureHash.sha256("DummyTransactionHash"), 0)
        // A ``StateAndRef`` pairs a ``StateRef`` with the state it points to.
        val ourStateAndRef: StateAndRef<IAmAlsoAState> = serviceHub.toStateAndRef<IAmAlsoAState>(ourStateRef)

        /**-----------------------------------------
         * GATHERING OTHER TRANSACTION COMPONENTS *
        -----------------------------------------**/
        progressTracker.currentStep = OTHER_TX_COMPONENTS

        // Output states are constructed from scratch.
        val ourOutputState: IAmAlsoAState = IAmAlsoAState("data", counterparty)
        // Or as copies of other states with some properties changed.
        val ourOtherOutputState: IAmAlsoAState = ourOutputState.copy(data = "new data")

        // We then need to pair our output state with a contract.
        val  ourOutput: StateAndContract = StateAndContract(ourOutputState, IAmAContract.CONTRACT_ID)

        // Commands pair a ``CommandData`` instance with a list of
        // public keys. To be valid, the transaction requires a signature
        // matching every public key in all of the transaction's commands.
        val commandData: IAmAContract.Commands.TypeOnlyCommand = IAmAContract.Commands.TypeOnlyCommand()
        val ourPubKey: PublicKey = serviceHub.myInfo.legalIdentitiesAndCerts.first().owningKey
        val counterpartyPubKey: PublicKey = counterparty.owningKey
        val requiredSigners: List<PublicKey> = listOf(ourPubKey, counterpartyPubKey)
        val ourCommand: Command<IAmAContract.Commands.TypeOnlyCommand> = Command(commandData, requiredSigners)

        // ``CommandData`` can either be:
        // 1. Of type ``TypeOnlyCommandData``, in which case it only
        //    serves to attach signers to the transaction and possibly
        //    fork the contract's verification logic.
        val typeOnlyCommandData: TypeOnlyCommandData = IAmAContract.Commands.TypeOnlyCommand()
        // 2. Include additional data which can be used by the contract
        //    during verification, alongside fulfilling the roles above.
        val commandDataWithData: CommandData = IAmAContract.Commands.CommandWithData("cotents for verification")

        // Attachments are identified by their hash.
        // The attachment with the corresponding hash must have been
        // uploaded ahead of time via the node's RPC interface.
        val ourAttachment: SecureHash = SecureHash.sha256("DummyAttachment")

        // Time windows can have a start and end time, or be open at either end.
        val ourTimeWindow: TimeWindow = TimeWindow.between(Instant.MIN, Instant.MAX)
        val ourAfter: TimeWindow = TimeWindow.fromOnly(Instant.MIN)
        val ourBefore: TimeWindow = TimeWindow.untilOnly(Instant.MAX)

        // We can also define a time window as an ``Instant`` +/- a time
        // tolerance (e.g. 30 seconds):
        val ourTimeWindow2: TimeWindow = TimeWindow.withTolerance(serviceHub.clock.instant(), 30.seconds)
        // Or as a start-time plus a duration:
        val ourTimeWindow3: TimeWindow = TimeWindow.fromStartAndDuration(serviceHub.clock.instant(), 30.seconds)

        /**-----------------------
         * TRANSACTION BUILDING *
        -----------------------**/
        progressTracker.currentStep = TX_BUILDING

        // If our transaction has input states or a time-window, we must instantiate it with a
        // notary.
        val txBuilder: TransactionBuilder = TransactionBuilder(specificNotary)

        // Otherwise, we can choose to instantiate it without one:
        val txBuilderNoNotary: TransactionBuilder = TransactionBuilder()

        // We add items to the transaction builder using ``TransactionBuilder.withItems``:
        txBuilder.withItems(
                // Inputs, as ``StateAndRef``s that reference the outputs of previous transactions
                ourStateAndRef,
                // Outputs, as ``StateAndContract``s
                ourOutput,
                // Commands, as ``Command``s
                ourCommand,
                // Attachments, as ``SecureHash``es
                ourAttachment,
                // A time-window, as ``TimeWindow``
                ourTimeWindow
        )

        // We can also add items using methods for the individual components.

        // The individual methods for adding input states and attachments:
        txBuilder.addInputState(ourStateAndRef)
        txBuilder.addAttachment(ourAttachment)

        // An output state can be added as a ``ContractState``, contract class name and notary.
        txBuilder.addOutputState(ourOutputState, IAmAContract.CONTRACT_ID, specificNotary)
        // We can also leave the notary field blank, in which case the transaction's default
        // notary is used.
        txBuilder.addOutputState(ourOutputState, IAmAContract.CONTRACT_ID)
        // Or we can add the output state as a ``TransactionState``, which already specifies
        // the output's contract and notary.
        val txState: TransactionState<IAmAlsoAState> = TransactionState(ourOutputState, IAmAContract.CONTRACT_ID, specificNotary)

        // Commands can be added as ``Command``s.
        txBuilder.addCommand(ourCommand)
        // Or as ``CommandData`` and a ``vararg PublicKey``.
        txBuilder.addCommand(commandData, ourPubKey, counterpartyPubKey)

        // We can set a time-window directly.
        txBuilder.setTimeWindow(ourTimeWindow)
        // Or as a start time plus a duration (e.g. 45 seconds).
        txBuilder.setTimeWindow(serviceHub.clock.instant(), 45.seconds)

        /**----------------------
         * TRANSACTION SIGNING *
        ----------------------**/
        progressTracker.currentStep = TX_SIGNING

        // We finalise the transaction by signing it, converting it into a
        // ``SignedTransaction``.
        val onceSignedTx: SignedTransaction = serviceHub.signInitialTransaction(txBuilder)
        // We can also sign the transaction using a different public key:
        val otherIdentity: PartyAndCertificate = serviceHub.keyManagementService.freshKeyAndCert(ourIdentityAndCert, false)
        val onceSignedTx2: SignedTransaction = serviceHub.signInitialTransaction(txBuilder, otherIdentity.owningKey)

        // If instead this was a ``SignedTransaction`` that we'd received
        // from a counterparty and we needed to sign it, we would add our
        // signature using:
        val twiceSignedTx: SignedTransaction = serviceHub.addSignature(onceSignedTx)
        // Or, if we wanted to use a different public key:
        val otherIdentity2: PartyAndCertificate = serviceHub.keyManagementService.freshKeyAndCert(ourIdentityAndCert, false)
        val twiceSignedTx2: SignedTransaction = serviceHub.addSignature(onceSignedTx, otherIdentity2.owningKey)

        // We can also generate a signature over the transaction without
        // adding it to the transaction itself. We may do this when
        // sending just the signature in a flow instead of returning the
        // entire transaction with our signature. This way, the receiving
        // node does not need to check we haven't changed anything in the
        // transaction.
        val sig: TransactionSignature = serviceHub.createSignature(onceSignedTx)
        // And again, if we wanted to use a different public key:
        val sig2: TransactionSignature = serviceHub.createSignature(onceSignedTx, otherIdentity2.owningKey)

        // In practice, however, the process of gathering every signature
        // but the first can be automated using ``CollectSignaturesFlow``.
        // See the "Gathering Signatures" section below.

        /**---------------------------
         * TRANSACTION VERIFICATION *
        ---------------------------**/
        progressTracker.currentStep = TX_VERIFICATION

        // Verifying a transaction will also verify every transaction in
        // the transaction's dependency chain, which will require
        // transaction data access on counterparty's node. The
        // ``SendTransactionFlow`` can be used to automate the sending and
        // data vending process. The ``SendTransactionFlow`` will listen
        // for data request until the transaction is resolved and verified
        // on the other side:
        subFlow(SendTransactionFlow(counterpartySession, twiceSignedTx))

        // Optional request verification to further restrict data access.
        subFlow(object : SendTransactionFlow(counterpartySession, twiceSignedTx) {
            override fun verifyDataRequest(dataRequest: FetchDataFlow.Request.Data) {
                // Extra request verification.
            }
        })

        // We can receive the transaction using ``ReceiveTransactionFlow``,
        // which will automatically download all the dependencies and verify
        // the transaction.
        val verifiedTransaction = subFlow(ReceiveTransactionFlow(counterpartySession))

        // We can also send and receive a `StateAndRef` dependency chain
        // and automatically resolve its dependencies.
        subFlow(SendStateAndRefFlow(counterpartySession, dummyStates))

        // On the receive side ...
        val resolvedStateAndRef = subFlow(ReceiveStateAndRefFlow<IAmAlsoAState>(counterpartySession))

        // We can now verify the transaction to ensure that it satisfies
        // the contracts of all the transaction's input and output states.
        twiceSignedTx.verify(serviceHub)

        // We'll often want to perform our own additional verification
        // too. Just because a transaction is valid based on the contract
        // rules and requires our signature doesn't mean we have to
        // sign it! We need to make sure the transaction represents an
        // agreement we actually want to enter into.

        // To do this, we need to convert our ``SignedTransaction``
        // into a ``LedgerTransaction``. This will use our ServiceHub
        // to resolve the transaction's inputs and attachments into
        // actual objects, rather than just references.
        val ledgerTx: LedgerTransaction = twiceSignedTx.toLedgerTransaction(serviceHub)

        // We can now perform our additional verification.
        val outputState: IAmAlsoAState = ledgerTx.outputsOfType<IAmAlsoAState>().single()
        if (outputState.data != "new data") {
            // ``FlowException`` is a special exception type. It will be
            // propagated back to any counterparty flows waiting for a
            // message from this flow, notifying them that the flow has
            // failed.
            throw FlowException("We expected different data.")
        }

        // Of course, if you are not a required signer on the transaction,
        // you have no power to decide whether it is valid or not. If it
        // collects signatures from all the required signers and is
        // contractually valid, it's a valid ledger update.

        /**-----------------------
         * GATHERING SIGNATURES *
        -----------------------**/
        progressTracker.currentStep = SIGS_GATHERING

        // The list of parties who need to sign a transaction is dictated
        // by the transaction's commands. Once we've signed a transaction
        // ourselves, we can automatically gather the signatures of the
        // other required signers using ``CollectSignaturesFlow``.
        // The responder flow will need to call ``SignTransactionFlow``.
        val fullySignedTx: SignedTransaction = subFlow(CollectSignaturesFlow(twiceSignedTx, setOf(counterpartySession, regulatorSession), SIGS_GATHERING.childProgressTracker()))

        /**-----------------------
         * VERIFYING SIGNATURES *
        -----------------------**/
        progressTracker.currentStep = VERIFYING_SIGS

        // We can verify that a transaction has all the required
        // signatures, and that they're all valid, by running:
        fullySignedTx.verifyRequiredSignatures()

        // If the transaction is only partially signed, we have to pass in
        // a list of the public keys corresponding to the missing
        // signatures, explicitly telling the system not to check them.
        onceSignedTx.verifySignaturesExcept(counterpartyPubKey)

        // We can also choose to only check the signatures that are
        // present. BE VERY CAREFUL - this function provides no guarantees
        // that the signatures are correct, or that none are missing.
        twiceSignedTx.checkSignaturesAreValid()

        /**-----------------------------
         * FINALISING THE TRANSACTION *
        -----------------------------**/
        progressTracker.currentStep = FINALISATION

        // We notarise the transaction and get it recorded in the vault of
        // the participants of all the transaction's states.
        val notarisedTx1: SignedTransaction = subFlow(FinalityFlow(fullySignedTx, FINALISATION.childProgressTracker()))
        // We can also choose to send it to additional parties who aren't one
        // of the state's participants.
        val additionalParties: Set<Party> = setOf(regulator)
        val notarisedTx2: SignedTransaction = subFlow(FinalityFlow(fullySignedTx, additionalParties, FINALISATION.childProgressTracker()))
    }
}

// ``ResponderFlow`` is our second flow, and will communicate with
// ``InitiatorFlow``.
// We mark ``ResponderFlow`` as an ``InitiatedByFlow``, meaning that it
// can only be started in response to a message from its initiating flow.
// That's ``InitiatorFlow`` in this case.
// Each node also has several flow pairs registered by default - see
// ``AbstractNode.installCoreFlows``.
@InitiatedBy(IAmAFlow::class)
class IAmAFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {

    companion object {
        object RECEIVING_AND_SENDING_DATA : Step("Sending data between parties.")
        object SIGNING : Step("Responding to CollectSignaturesFlow.")
        object FINALISATION : Step("Finalising a transaction.")

        fun tracker() = ProgressTracker(
                RECEIVING_AND_SENDING_DATA,
                SIGNING,
                FINALISATION
        )
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call() {
        // The ``ResponderFlow` has all the same APIs available. It looks
        // up network information, sends and receives data, and constructs
        // transactions in exactly the same way.

        /**-----------------------------
         * SENDING AND RECEIVING DATA *
        -----------------------------**/
        progressTracker.currentStep = RECEIVING_AND_SENDING_DATA

        // We need to respond to the messages sent by the initiator:
        // 1. They sent us an ``Any`` instance
        // 2. They waited to receive an ``Integer`` instance back
        // 3. They sent a ``String`` instance and waited to receive a
        //    ``Boolean`` instance back
        // Our side of the flow must mirror these calls.
        val any: Any = counterpartySession.receive<Any>().unwrap { data -> data }
        val string: String = counterpartySession.sendAndReceive<String>(99).unwrap { data -> data }
        counterpartySession.send(true)

        /**----------------------------------------
         * RESPONDING TO COLLECT_SIGNATURES_FLOW *
        ----------------------------------------**/
        progressTracker.currentStep = SIGNING

        // The responder will often need to respond to a call to
        // ``CollectSignaturesFlow``. It does so my invoking its own
        // ``SignTransactionFlow`` subclass.
        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // Any additional checking we see fit...
                val outputState = stx.tx.outputsOfType<IAmAlsoAState>().single()
                assert(outputState.data == "new data")
            }
        }

        subFlow(signTransactionFlow)

        /**-----------------------------
         * FINALISING THE TRANSACTION *
        -----------------------------**/
        progressTracker.currentStep = FINALISATION

        // Nothing to do here! As long as some other party calls
        // ``FinalityFlow``, the recording of the transaction on our node
        // we be handled automatically.
    }
}