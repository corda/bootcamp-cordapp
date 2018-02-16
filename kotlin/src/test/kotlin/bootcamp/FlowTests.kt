package bootcamp

import net.corda.testing.core.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.startFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("bootcamp"))
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `transaction constructed by flow uses the correct notary`() {
        val flow = TokenFlow(b.info.chooseIdentity(), 99)
        val future = a.services.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.outputStates.size)
        val output = signedTransaction.tx.outputs.single()

        assertEquals(network.notaryNodes.single().info.chooseIdentity(), output.notary)
    }

    @Test
    fun `transaction constructed by flow has one TokenState output with the correct amount and recipient`() {
        val flow = TokenFlow(b.info.chooseIdentity(), 99)
        val future = a.services.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.outputStates.size)
        val output = signedTransaction.tx.outputsOfType<TokenState>().single()

        assertEquals(b.info.chooseIdentity(), output.recipient)
        assertEquals(99, output.amount)
    }

    @Test
    fun `transaction constructed by flow has one output using the correct contract`() {
        val flow = TokenFlow(b.info.chooseIdentity(), 99)
        val future = a.services.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.outputStates.size)
        val output = signedTransaction.tx.outputs.single()

        assertEquals("bootcamp.TokenContract", output.contract)
    }

    @Test
    fun `transaction constructed by flow has one Issue command`() {
        val flow = TokenFlow(b.info.chooseIdentity(), 99)
        val future = a.services.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.commands.size)
        val command = signedTransaction.tx.commands.single()

        assert(command.value is TokenContract.Issue)
    }

    @Test
    fun `transaction constructed by flow has one command with the issuer as a signer`() {
        val flow = TokenFlow(b.info.chooseIdentity(), 99)
        val future = a.services.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.commands.size)
        val command = signedTransaction.tx.commands.single()

        assertEquals(setOf(a.info.chooseIdentity().owningKey), command.signers.toSet())
    }

    @Test
    fun `transaction constructed by flow has no inputs, attachments or timewindows`() {
        val flow = TokenFlow(b.info.chooseIdentity(), 99)
        val future = a.services.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(0, signedTransaction.tx.inputs.size)
        assertEquals(0, signedTransaction.tx.attachments.size)
        assertEquals(null, signedTransaction.tx.timeWindow)
    }
}