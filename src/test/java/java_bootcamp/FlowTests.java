package java_bootcamp;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionState;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;

    @Before
    public void setup() {
        network = new MockNetwork(ImmutableList.of("java_bootcamp"));
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

//    @Test
//    public void transactionConstructedByFlowUsesTheCorrectNotary() throws Exception {
//        TokenIssueFlow flow = new TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
//        TransactionState output = signedTransaction.getTx().getOutputs().get(0);
//
//        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output.getNotary());
//    }

//    @Test
//    public void transactionConstructedByFlowHasOneTokenStateOutputWithTheCorrectAmountAndOwner() throws Exception {
//        TokenIssueFlow flow = new TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
//        TokenState output = signedTransaction.getTx().outputsOfType(TokenState.class).get(0);
//
//        assertEquals(nodeB.getInfo().getLegalIdentities().get(0), output.getOwner());
//        assertEquals(99, output.getAmount());
//    }

//    @Test
//    public void transactionConstructedByFlowHasOneOutputUsingTheCorrectContract() throws Exception {
//        TokenIssueFlow flow = new TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
//        TransactionState output = signedTransaction.getTx().getOutputs().get(0);
//
//        assertEquals("java_bootcamp.TokenContract", output.getContract());
//    }

//    @Test
//    public void transactionConstructedByFlowHasOneIssueCommand() throws Exception {
//        TokenIssueFlow flow = new TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getCommands().size());
//        Command command = signedTransaction.getTx().getCommands().get(0);
//
//        assert(command.getValue() instanceof TokenContract.Issue);
//    }

//    @Test
//    public void transactionConstructedByFlowHasOneCommandWithTheIssueAsASigner() throws Exception {
//        TokenIssueFlow flow = new TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getCommands().size());
//        Command command = signedTransaction.getTx().getCommands().get(0);
//
//        assertEquals(1, command.getSigners().size());
//        assert(command.getSigners().contains(nodeA.getInfo().getLegalIdentities().get(0).getOwningKey()));
//    }

//    @Test
//    public void transactionConstructedByFlowHasNoInputsAttachmentsOrTimeWindows() throws Exception {
//        TokenIssueFlow flow = new TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(0, signedTransaction.getTx().getInputs().size());
//        // The single attachment is the contract attachment.
//        assertEquals(1, signedTransaction.getTx().getAttachments().size());
//        assertEquals(null, signedTransaction.getTx().getTimeWindow());
//    }
}