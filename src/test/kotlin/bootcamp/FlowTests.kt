package bootcamp

import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.node.internal.StartedNode
import net.corda.testing.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetwork.MockNode
import net.corda.testing.setCordappPackages
import net.corda.testing.unsetCordappPackages
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FlowTests {
    lateinit var network: MockNetwork
    lateinit var node: StartedNode<MockNode>

    @Before
    fun setup() {
        setCordappPackages("bootcamp")
        network = MockNetwork()
        val nodes = network.createSomeNodes(1)
        node = nodes.partyNodes.single()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
        unsetCordappPackages()
    }

    @Test
    fun `integration test`() {
        val flow = SomethingFlow()
        val future = node.services.startFlow(flow).resultFuture
        network.runNetwork()
        future.getOrThrow()

        // We check the recorded IOU in both vaults.
        node.database.transaction {
            val somethings = node.services.vaultService.queryBy<SomethingState>().states
            assertEquals(1, somethings.size)
            val something = somethings.single().state.data
            assertEquals(something.owner, node.info.chooseIdentity())
        }
    }
}