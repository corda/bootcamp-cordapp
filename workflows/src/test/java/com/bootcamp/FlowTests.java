package com.bootcamp;

import com.bootcamp.contracts.TokenContract;
import com.bootcamp.flows.TokenIssueFlow;
import com.bootcamp.states.TokenState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class FlowTests {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;
    private StartedMockNode nodeC;


    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.bootcamp.contracts"),
                TestCordapp.findCordapp("com.bootcamp.flows")))
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB")))));
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);
        nodeC = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

//    @Test
//    public void transactionConstructedByFlowUsesTheCorrectNotary() throws Exception {
//        TokenIssueFlow.TokenIssueFlowInitiator flow = new TokenIssueFlow.TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
//        TransactionState output = signedTransaction.getTx().getOutputs().get(0);
//
//        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output.getNotary());
//    }
//
//    @Test
//    public void transactionConstructedByFlowHasOneTokenStateOutputWithTheCorrectAmountAndOwner() throws Exception {
//        TokenIssueFlow.TokenIssueFlowInitiator flow = new TokenIssueFlow.TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
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
//
//    @Test
//    public void transactionConstructedByFlowHasOneOutputUsingTheCorrectContract() throws Exception {
//        TokenIssueFlow.TokenIssueFlowInitiator flow = new TokenIssueFlow.TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
//        TransactionState output = signedTransaction.getTx().getOutputs().get(0);
//
//        assertEquals("com.bootcamp.contracts.TokenContract", output.getContract());
//    }
//
//    @Test
//    public void transactionConstructedByFlowHasOneIssueCommand() throws Exception {
//        TokenIssueFlow.TokenIssueFlowInitiator flow = new TokenIssueFlow.TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getCommands().size());
//        Command command = signedTransaction.getTx().getCommands().get(0);
//
//        assert (command.getValue() instanceof TokenContract.Commands.Issue);
//    }
//
//    @Test
//    public void transactionConstructedByFlowHasOneCommandWithTheIssuerAndTheOwnerAsASigners() throws Exception {
//        TokenIssueFlow.TokenIssueFlowInitiator flow = new TokenIssueFlow.TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(1, signedTransaction.getTx().getCommands().size());
//        Command command = signedTransaction.getTx().getCommands().get(0);
//
//        assertEquals(2, command.getSigners().size());
//        assertTrue(command.getSigners().contains(nodeA.getInfo().getLegalIdentities().get(0).getOwningKey()));
//        assertTrue(command.getSigners().contains(nodeB.getInfo().getLegalIdentities().get(0).getOwningKey()));
//    }
//
//    @Test
//    public void transactionConstructedByFlowHasNoInputsAttachmentsOrTimeWindows() throws Exception {
//        TokenIssueFlow.TokenIssueFlowInitiator flow = new TokenIssueFlow.TokenIssueFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 99);
//        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
//        network.runNetwork();
//        SignedTransaction signedTransaction = future.get();
//
//        assertEquals(0, signedTransaction.getTx().getInputs().size());
//        // The single attachment is the contract attachment.
//        assertEquals(1, signedTransaction.getTx().getAttachments().size());
//        assertNull(signedTransaction.getTx().getTimeWindow());
//    }
}
