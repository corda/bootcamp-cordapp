package bootcamp

import net.corda.core.contracts.ContractState
import net.corda.testing.ALICE
import net.corda.testing.BOB
import org.junit.Test
import kotlin.test.assertEquals

class StateTests {
    @Test
    fun `TokenState has issuer, recipient and amount fields of the correct type`() {
        TokenState(ALICE, BOB, 1)
    }

    @Test
    fun `TokenState implements ContractState`() {
        assert(TokenState(ALICE, BOB, 1) is ContractState)
    }

    @Test
    fun `TokenState has two participants, the issuer and the recipient`() {
        val tokenState = TokenState(ALICE, BOB, 1)
        assertEquals(setOf(ALICE, BOB), tokenState.participants.toSet())
    }
}