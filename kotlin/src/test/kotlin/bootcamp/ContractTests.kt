package bootcamp

import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.DummyCommandData
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractTests {
    private val alice = TestIdentity(CordaX500Name("Alice", "", "GB"))
    private val bob = TestIdentity(CordaX500Name("Bob", "", "GB"))
    private val ledgerServices = MockServices(TestIdentity(CordaX500Name("TestId", "", "GB")))
    private val tokenState = TokenState(alice.party, bob.party, 1)

    @Test
    fun `TokenContract requires zero inputs in the transaction`() {
        ledgerServices.ledger {
            transaction {
                // Has an input, will fail.
                input(TokenContract.ID, tokenState)
                output(TokenContract.ID, tokenState)
                command(alice.publicKey, TokenContract.Issue)
                fails()
            }
            transaction {
                // Has no input, will verify.
                output(TokenContract.ID, tokenState)
                command(alice.publicKey, TokenContract.Issue)
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract requires one output in the transaction`() {
        ledgerServices.ledger {
            transaction {
                // Has two outputs, will fail.
                output(TokenContract.ID, tokenState)
                output(TokenContract.ID, tokenState)
                command(alice.publicKey, TokenContract.Issue)
                fails()
            }
            transaction {
                // Has one input, will verify.
                output(TokenContract.ID, tokenState)
                command(alice.publicKey, TokenContract.Issue)
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract requires one command in the transaction`() {
        ledgerServices.ledger {
            transaction {
                output(TokenContract.ID, tokenState)
                // Has two commands, will fail.
                command(alice.publicKey, TokenContract.Issue)
                command(alice.publicKey, TokenContract.Issue)
                fails()
            }
            transaction {
                output(TokenContract.ID, tokenState)
                // Has one command, will verify.
                command(alice.publicKey, TokenContract.Issue)
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract requires the transaction's output to be a TokenState`() {
        ledgerServices.ledger {
            transaction {
                // Has wrong output type, will fail.
                output(TokenContract.ID, DummyState())
                command(alice.publicKey, TokenContract.Issue)
                fails()
            }
            transaction {
                // Has correct output type, will verify.
                output(TokenContract.ID, tokenState)
                command(alice.publicKey, TokenContract.Issue)
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract requires the transaction's output TokenState to have a positive amount`() {
        val zeroTokenState = TokenState(alice.party, bob.party, -1)
        val negativeTokenState = TokenState(alice.party, bob.party, -1)
        val positiveTokenState = TokenState(alice.party, bob.party, 2)

        ledgerServices.ledger {
            transaction {
                // Has zero-amount TokenState, will fail.
                output(TokenContract.ID, zeroTokenState)
                command(alice.publicKey, TokenContract.Issue)
                fails()
            }
            transaction {
                // Has negative-amount TokenState, will fail.
                output(TokenContract.ID, negativeTokenState)
                command(alice.publicKey, TokenContract.Issue)
                fails()
            }
            transaction {
                // Has positive-amount TokenState, will verify.
                output(TokenContract.ID, tokenState)
                command(alice.publicKey, TokenContract.Issue)
                verifies()
            }
            transaction {
                // Also has positive-amount TokenState, will verify.
                output(TokenContract.ID, positiveTokenState)
                command(alice.publicKey, TokenContract.Issue)
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract requires the transaction's command to be an Issue command`() {
        ledgerServices.ledger {
            transaction {
                // Has wrong command type, will fail.
                output(TokenContract.ID, tokenState)
                command(alice.publicKey, DummyCommandData)
                fails()
            }
            transaction {
                // Has correct command type, will verify.
                output(TokenContract.ID, tokenState)
                command(alice.publicKey, TokenContract.Issue)
                verifies()
            }
        }
    }

    @Test
    fun `TokenContract requires the issuer to be a required signer in the transaction`() {
        val tokenStateWhereBobIsIssuer = TokenState(bob.party, alice.party, 1)

        ledgerServices.ledger {
            transaction {
                // Issuer is not a required signer, will fail.
                output(TokenContract.ID, tokenState)
                command(bob.publicKey, DummyCommandData)
                fails()
            }
            transaction {
                // Issuer is also not a required signer, will fail.
                output(TokenContract.ID, tokenStateWhereBobIsIssuer)
                command(alice.publicKey, DummyCommandData)
                fails()
            }
            transaction {
                // Issuer is a required signer, will verify.
                output(TokenContract.ID, tokenState)
                command(alice.publicKey, TokenContract.Issue)
                verifies()
            }
            transaction {
                // Issuer is also a required signer, will verify.
                output(TokenContract.ID, tokenStateWhereBobIsIssuer)
                command(bob.publicKey, TokenContract.Issue)
                verifies()
            }
        }
    }
}