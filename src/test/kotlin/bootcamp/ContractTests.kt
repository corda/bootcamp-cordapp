package bootcamp

import net.corda.testing.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class ContractTests {
    @Before
    fun setup() {
        setCordappPackages("bootcamp")
    }

    @After
    fun tearDown() {
        unsetCordappPackages()
    }

    @Test
    fun `TokenContract imposes constraint that there are zero inputs`() {
        val tokenState = TokenState(ALICE, BOB, 1)

        ledger {
            transaction {
                // Has an input, will fail.
                input(TokenContract.ID) { tokenState }
                output(TokenContract.ID) { tokenState }
                command(ALICE_PUBKEY, BOB_PUBKEY) { TokenContract.Issue }
                fails()
            }
            transaction {
                // Has no input, will verify.
                output(TokenContract.ID) { tokenState }
                command(ALICE_PUBKEY, BOB_PUBKEY) { TokenContract.Issue }
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract imposes constraint that there is one output`() {
        TODO()
    }

    @Test
    fun `TokenContract imposes constraint that there is one command`() {
        TODO()
    }

    @Test
    fun `TokenContract imposes constraint that the output is a TokenState`() {
        TODO()
    }

    @Test
    fun `TokenContract imposes constraint that the output TokenState has a positive amount`() {
        TODO()
    }

    @Test
    fun `TokenContract imposes constraint that the command is an Issue command`() {
        TODO()
    }

    @Test
    fun `TokenContract imposes constraint that the issuer is a required signer`() {
        TODO()
    }
}