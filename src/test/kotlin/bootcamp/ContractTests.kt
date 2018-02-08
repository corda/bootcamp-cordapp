package bootcamp

import net.corda.testing.*
import net.corda.testing.contracts.DummyState
import org.junit.After
import org.junit.Before
import org.junit.Test

class ContractTests {
    private val tokenState = TokenState(ALICE, BOB, 1)

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
        ledger {
            transaction {
                // Has an input, will fail.
                input(TokenContract.ID) { tokenState }
                output(TokenContract.ID) { tokenState }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                fails()
            }
            transaction {
                // Has no input, will verify.
                output(TokenContract.ID) { tokenState }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract imposes constraint that there is one output`() {
        ledger {
            transaction {
                // Has two outputs, will fail.
                output(TokenContract.ID) { tokenState }
                output(TokenContract.ID) { tokenState }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                fails()
            }
            transaction {
                // Has one input, will verify.
                output(TokenContract.ID) { tokenState }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract imposes constraint that there is one command`() {
        ledger {
            transaction {
                output(TokenContract.ID) { tokenState }
                // Has two commands, will fail.
                command(ALICE_PUBKEY) { TokenContract.Issue }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                fails()
            }
            transaction {
                output(TokenContract.ID) { tokenState }
                // Has one command, will verify.
                command(ALICE_PUBKEY) { TokenContract.Issue }
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract imposes constraint that the output is a TokenState`() {
        ledger {
            transaction {
                // Has wrong output type, will fail.
                output(TokenContract.ID) { DummyState() }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                fails()
            }
            transaction {
                // Has correct output type, will verify.
                output(TokenContract.ID) { tokenState }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract imposes constraint that the output TokenState has a positive amount`() {
        val zeroTokenState = TokenState(ALICE, BOB, -1)
        val negativeTokenState = TokenState(ALICE, BOB, -1)
        val positiveTokenState = TokenState(ALICE, BOB, 2)

        ledger {
            transaction {
                // Has zero-amount TokenState, will fail.
                output(TokenContract.ID) { zeroTokenState }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                fails()
            }
            transaction {
                // Has negative-amount TokenState, will fail.
                output(TokenContract.ID) { negativeTokenState }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                fails()
            }
            transaction {
                // Has positive-amount TokenState, will verify.
                output(TokenContract.ID) { tokenState }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                verifies()
            }
            transaction {
                // Also has positive-amount TokenState, will verify.
                output(TokenContract.ID) { positiveTokenState }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract imposes constraint that the command is an Issue command`() {
        ledger {
            transaction {
                // Has wrong command type, will fail.
                output(TokenContract.ID) { tokenState }
                command(ALICE_PUBKEY) { DummyCommandData }
                fails()
            }
            transaction {
                // Has correct command type, will verify.
                output(TokenContract.ID) { tokenState }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract imposes constraint that the issuer is a required signer`() {
        val tokenStateWhereBobIsIssuer = TokenState(BOB, ALICE, 1)

        ledger {
            transaction {
                // Issuer is not a required signer, will fail.
                output(TokenContract.ID) { tokenState }
                command(BOB_PUBKEY) { DummyCommandData }
                fails()
            }
            transaction {
                // Issuer is also not a required signer, will fail.
                output(TokenContract.ID) { tokenStateWhereBobIsIssuer }
                command(ALICE_PUBKEY) { DummyCommandData }
                fails()
            }
            transaction {
                // Issuer is a required signer, will verify.
                output(TokenContract.ID) { tokenState }
                command(ALICE_PUBKEY) { TokenContract.Issue }
                verifies()
            }
            transaction {
                // Issuer is also a required signer, will verify.
                output(TokenContract.ID) { tokenStateWhereBobIsIssuer }
                command(BOB_PUBKEY) { TokenContract.Issue }
                verifies()
            }
        }
    }
}