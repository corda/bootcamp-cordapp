package bootcamp;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import org.junit.Test;

import java.util.List;

import static net.corda.testing.driver.Driver.driver;
import static org.junit.Assert.assertEquals;

public class IntegrationTest {
    private final TestIdentity partyAIdentity = new TestIdentity(new CordaX500Name("PartyA", "London", "GB"));
    private final TestIdentity partyBIdentity = new TestIdentity(new CordaX500Name("PartyB", "New York", "US"));
    private final TestIdentity partyCIdentity = new TestIdentity(new CordaX500Name("PartyC", "Mumbai", "IN"));

    @Test
    public void issueTest() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true), dsl -> {
            // Start a pair of nodes and wait for them both to be ready.
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(partyAIdentity.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(partyBIdentity.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(partyCIdentity.getName()))
            );

            try {
                NodeHandle partyAHandle = handleFutures.get(0).get();
                NodeHandle partyBHandle = handleFutures.get(1).get();
                NodeHandle partyCHandle = handleFutures.get(2).get();

                Party partyA = partyAHandle.getNodeInfo().getLegalIdentities().get(0);
                Party partyB = partyBHandle.getNodeInfo().getLegalIdentities().get(0);

//                // Run issue transaction using rpc
//                partyAHandle.getRpc().startTrackedFlowDynamic(TokenIssueFlowInitiator.class, partyB, 100)
//                        .getReturnValue().get();
//
//                // Query Node A
//                Vault.Page<TokenState> tokenStates_A = partyAHandle.getRpc().vaultQuery(TokenState.class);
//                assertEquals(1, tokenStates_A.getStates().size());
//
//                TokenState tokenState_A = tokenStates_A.getStates().get(0).getState().getData();
//                assertEquals(partyA, tokenState_A.getIssuer());
//                assertEquals(partyB, tokenState_A.getOwner());
//                assertEquals(100, tokenState_A.getAmount());
//
//
//                // Query Node B
//                Vault.Page<TokenState> tokenStates_B = partyBHandle.getRpc().vaultQuery(TokenState.class);
//                assertEquals(1, tokenStates_B.getStates().size());
//
//                TokenState tokenState_B = tokenStates_B.getStates().get(0).getState().getData();
//                assertEquals(partyA, tokenState_B.getIssuer());
//                assertEquals(partyB, tokenState_B.getOwner());
//                assertEquals(100, tokenState_B.getAmount());
//
//
//                //Query Node C
//                Vault.Page<TokenState> tokenStates_C = partyCHandle.getRpc().vaultQuery(TokenState.class);
//                assertEquals(0, tokenStates_C.getStates().size());

            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test: ", e);
            }

            return null;
        });
    }
}
