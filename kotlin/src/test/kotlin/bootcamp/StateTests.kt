package bootcamp

import net.corda.core.contracts.ContractState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Test
import kotlin.test.assertEquals

class StateTests {
    private val alice = TestIdentity(CordaX500Name("Alice", "", "GB")).party
    private val bob = TestIdentity(CordaX500Name("Bob", "", "GB")).party

    @Test
    fun `TokenState has issuer, recipient and amount fields of the correct type`() {
        TokenState(alice, bob, 1)
    }

    @Test
    fun `TokenState implements ContractState`() {
        assert(TokenState(alice, bob, 1) is ContractState)
    }

    @Test
    fun `TokenState has two participants, the issuer and the recipient`() {
        val tokenState = TokenState(alice, bob, 1)
        assertEquals(setOf(alice, bob), tokenState.participants.toSet())
    }
}