package bootcamp;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class FinalizeFlowTests {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;
    private StateAndRef<TokenState> inputTokenStateRef;

    @Before
    public void setup() throws Exception {
        network = new MockNetwork(
                new MockNetworkParameters(
                        Collections.singletonList(TestCordapp.findCordapp("bootcamp"))
                )
        );
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);

        // issue a token on nodeA
        TokenIssueFlowInitiator flow = new TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 80);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();

        SignedTransaction tokenIssueTx = future.get(); // transaction from issuance
        inputTokenStateRef = tokenIssueTx.getTx().outRef(0);
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void transactionConstructedByFlowUsesTheCorrectNotary() throws Exception {
        TokenFinalizeFlowInitiator flow = new TokenFinalizeFlowInitiator(inputTokenStateRef);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), signedTransaction.getNotary());
    }

    @Test
    public void transactionConstructedByFlowHasOneInput() throws Exception {
        TokenFinalizeFlowInitiator flow = new TokenFinalizeFlowInitiator(inputTokenStateRef);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getInputs().size());
    }

    @Test
    public void transactionConstructedByFlowHasZeroOutputs() throws Exception {
        TokenFinalizeFlowInitiator flow = new TokenFinalizeFlowInitiator(inputTokenStateRef);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(0, signedTransaction.getTx().getOutputs().size());
    }

    @Test
    public void transactionConstructedByFlowHasOneFinalizeCommand() throws Exception {
        TokenFinalizeFlowInitiator flow = new TokenFinalizeFlowInitiator(inputTokenStateRef);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getCommands().size());
        Command command = signedTransaction.getTx().getCommands().get(0);

        assert (command.getValue() instanceof TokenContract.Commands.Finalize);
    }

    @Test
    public void transactionConstructedByFlowHasOneCommandWithIssuerOwnerAsSigners() throws Exception {
        TokenFinalizeFlowInitiator flow = new TokenFinalizeFlowInitiator(inputTokenStateRef);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getCommands().size());
        Command command = signedTransaction.getTx().getCommands().get(0);

        assertEquals(2, command.getSigners().size());
        assertTrue(command.getSigners().contains(nodeA.getInfo().getLegalIdentities().get(0).getOwningKey())); // issuer
        assertTrue(command.getSigners().contains(nodeB.getInfo().getLegalIdentities().get(0).getOwningKey())); // owner
    }

    @Test
    public void transactionConstructedByFlowHasNoAttachmentsOrTimeWindows() throws Exception {
        TokenFinalizeFlowInitiator flow = new TokenFinalizeFlowInitiator(inputTokenStateRef);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getAttachments().size());
        assertNull(signedTransaction.getTx().getTimeWindow());
    }

    @Test
    public void transactionConstructedViaStringConstructorWorks() throws Exception {
        TokenFinalizeFlowInitiator flow = new TokenFinalizeFlowInitiator(inputTokenStateRef.getRef().getTxhash().toString(), inputTokenStateRef.getRef().getIndex());
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();
    }
}