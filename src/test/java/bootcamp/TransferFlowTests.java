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
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class TransferFlowTests {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;
    private StartedMockNode nodeC;
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
        nodeC = network.createPartyNode(null);

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
        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(inputTokenStateRef, nodeC.getInfo().getLegalIdentities().get(0), 80);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        TransactionState output = signedTransaction.getTx().getOutputs().get(0);

        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output.getNotary());
    }

    @Test
    public void transactionConstructedByFlowWithOneOutputHasCorrectIssuerAmountAndOwner() throws Exception {
        // one output
        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(inputTokenStateRef, nodeC.getInfo().getLegalIdentities().get(0), 80);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getOutputs().size()); // assert one output
        TokenState inputToken = inputTokenStateRef.getState().getData();
        TokenState output1 = (TokenState) signedTransaction.getTx().getOutput(0);

        assertEquals(inputToken.getIssuer(), output1.getIssuer());
        assertEquals(inputToken.getAmount(), output1.getAmount());
        assertEquals(nodeC.getInfo().getLegalIdentities().get(0), output1.getOwner());
    }

    @Test
    public void transactionConstructedByFlowWithTwoOutputHasCorrectIssuerAmountAndOwner() throws Exception {
        int transferAmount = 60;
        // two output
        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(inputTokenStateRef, nodeC.getInfo().getLegalIdentities().get(0), transferAmount);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(2, signedTransaction.getTx().getOutputs().size()); // assert two outputs
        TokenState inputToken = inputTokenStateRef.getState().getData();
        int inputAmount = inputToken.getAmount();
        TokenState output1 = (TokenState) signedTransaction.getTx().getOutput(0);
        TokenState output2 = (TokenState) signedTransaction.getTx().getOutput(1);

        assertEquals(inputToken.getIssuer(), output1.getIssuer());
        assertEquals(transferAmount, output1.getAmount());
        assertEquals(nodeC.getInfo().getLegalIdentities().get(0), output1.getOwner()); // transfer amount goes to nodeC

        assertEquals(inputToken.getIssuer(), output2.getIssuer());
        assertEquals(inputToken.getAmount() - transferAmount, output2.getAmount());
        assertEquals(nodeB.getInfo().getLegalIdentities().get(0), output2.getOwner()); // leftover goes to nodeB
    }

    @Test
    public void transactionConstructedByFlowHasOutputUsingCorrectContract() throws Exception {
        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(inputTokenStateRef, nodeC.getInfo().getLegalIdentities().get(0), 80);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        TransactionState output = signedTransaction.getTx().getOutputs().get(0);

        assertEquals("bootcamp.TokenContract", output.getContract());
    }

    @Test
    public void transactionConstructedByFlowHasOneTransferCommand() throws Exception {
        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(inputTokenStateRef, nodeC.getInfo().getLegalIdentities().get(0), 80);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getCommands().size());
        Command command = signedTransaction.getTx().getCommands().get(0);

        assert (command.getValue() instanceof TokenContract.Commands.Transfer);
    }

    @Test
    public void transactionConstructedByFlowHasOneCommandWithIssuerOwnerAndTargetAsSigners() throws Exception {
        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(inputTokenStateRef, nodeC.getInfo().getLegalIdentities().get(0), 80);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getCommands().size());
        Command command = signedTransaction.getTx().getCommands().get(0);

        assertEquals(3, command.getSigners().size());
        assertTrue(command.getSigners().contains(nodeA.getInfo().getLegalIdentities().get(0).getOwningKey())); // issuer
        assertTrue(command.getSigners().contains(nodeB.getInfo().getLegalIdentities().get(0).getOwningKey())); // owner
        assertTrue(command.getSigners().contains(nodeC.getInfo().getLegalIdentities().get(0).getOwningKey())); // target
    }

    @Test
    public void transactionConstructedByFlowHasNoAttachmentsOrTimeWindows() throws Exception {
        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(inputTokenStateRef, nodeC.getInfo().getLegalIdentities().get(0), 80);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getAttachments().size());
        assertNull(signedTransaction.getTx().getTimeWindow());
    }

    @Test
    public void transactionConstructedViaStringConstructorWorks() throws Exception {
        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(inputTokenStateRef.getRef().getTxhash().toString(), nodeC.getInfo().getLegalIdentities().get(0), 80);
        CordaFuture<SignedTransaction> future = nodeB.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();
    }
}