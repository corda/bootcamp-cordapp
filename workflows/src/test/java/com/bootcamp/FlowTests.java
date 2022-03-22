package com.bootcamp;

import com.bootcamp.flows.CreateAndIssueEvolvableToken;
import com.bootcamp.flows.CreateAndIssueFixedToken;
import com.bootcamp.states.StateTokenType;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.NetworkParameters;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;
    private StartedMockNode nodeC;

    private NetworkParameters testNetworkParameters =
            new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(),1, new LinkedHashMap<>());
    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.bootcamp.contracts"),
                TestCordapp.findCordapp("com.bootcamp.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")))
                .withNetworkParameters(testNetworkParameters)
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

    @Test
    public void CreateAndIssueFixedToken() throws Exception {
        CreateAndIssueFixedToken flow = new CreateAndIssueFixedToken("GoldCoin", 100L, nodeB.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();
        StateAndRef<FungibleToken> asset = nodeB.getServices().getVaultService().queryBy(FungibleToken.class).getStates().get(0);
        Long amount = asset.getState().getData().getAmount().getQuantity();
        assertEquals(100, amount.intValue()/100);
    }

    @Test
    public void CreateAndIssueEvolvableToken() throws Exception {
        String descriptionOfToken = "This is a house in New York";
        CreateAndIssueEvolvableToken.CreateEvolvableToken flow =
                new CreateAndIssueEvolvableToken.CreateEvolvableToken(descriptionOfToken);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();

        //Verify for creation
        StateAndRef<StateTokenType> tokentype = nodeA.getServices().getVaultService().queryBy(StateTokenType.class).getStates().get(0);
        assertEquals(descriptionOfToken,tokentype.getState().getData().getImportantInformationThatMayChange());

        UniqueIdentifier id = tokentype.getState().getData().getLinearId();
        System.out.println();
        System.out.println("The token created has Id: "+ id.toString());
        System.out.println();
        CreateAndIssueEvolvableToken.IssueEvolvableToken flow2 = new CreateAndIssueEvolvableToken.IssueEvolvableToken(id.toString(),nodeB.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future2 = nodeA.startFlow(flow2);
        network.runNetwork();

        //Verify for creation
        StateAndRef<NonFungibleToken> recievedToken = nodeB.getServices().getVaultService().queryBy(NonFungibleToken.class).getStates().get(0);
        String recievedTokenID = recievedToken.getState().getData().getTokenType().getTokenIdentifier();
        assertEquals(id.toString(),recievedTokenID);
    }


}
