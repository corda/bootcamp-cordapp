//package bootcamp
//
//import com.nhaarman.mockito_kotlin.doReturn
//import com.nhaarman.mockito_kotlin.whenever
//import net.corda.core.identity.CordaX500Name
//import net.corda.node.services.api.IdentityServiceInternal
//import net.corda.testing.contracts.DummyState
//import net.corda.testing.core.DummyCommandData
//import net.corda.testing.core.TestIdentity
//import net.corda.testing.internal.rigorousMock
//import net.corda.testing.node.MockServices
//import net.corda.testing.node.ledger
//import org.junit.Test
//
//class ContractTestsV3 {
//    private val alice = TestIdentity(CordaX500Name("Alice", "", "GB")).party
//    private val bob = TestIdentity(CordaX500Name("Bob", "", "GB")).party
//    private val alicePubKey = alice.owningKey
//    private val bobPubKey = bob.owningKey
//    private val identityService = rigorousMock<IdentityServiceInternal>().also {
//        doReturn(alice).whenever(it).partyFromKey(alicePubKey)
//        doReturn(bob).whenever(it).partyFromKey(bobPubKey)
//    }
//    private val ledgerServices = MockServices(listOf("bootcamp"), identityService, TestIdentity(CordaX500Name("TestId", "", "GB")))
//    private val tokenState = TokenState(alice, bob, 1)
//
//    @Test
//    fun `TokenContract imposes constraint that there are zero inputs`() {
//        ledgerServices.ledger {
//            transaction {
//                // Has an input, will fail.
//                input(TokenContract.ID, tokenState)
//                output(TokenContract.ID, tokenState)
//                command(alicePubKey, TokenContract.Issue)
//                fails()
//            }
//            transaction {
//                // Has no input, will verify.
//                output(TokenContract.ID, tokenState)
//                command(alicePubKey, TokenContract.Issue)
//                verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `TokenContract imposes constraint that there is one output`() {
//        ledgerServices.ledger {
//            transaction {
//                // Has two outputs, will fail.
//                output(TokenContract.ID, tokenState)
//                output(TokenContract.ID, tokenState)
//                command(alicePubKey, TokenContract.Issue)
//                fails()
//            }
//            transaction {
//                // Has one input, will verify.
//                output(TokenContract.ID, tokenState)
//                command(alicePubKey, TokenContract.Issue)
//                verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `TokenContract imposes constraint that there is one command`() {
//        ledgerServices.ledger {
//            transaction {
//                output(TokenContract.ID, tokenState)
//                // Has two commands, will fail.
//                command(alicePubKey, TokenContract.Issue)
//                command(alicePubKey, TokenContract.Issue)
//                fails()
//            }
//            transaction {
//                output(TokenContract.ID, tokenState)
//                // Has one command, will verify.
//                command(alicePubKey, TokenContract.Issue)
//                verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `TokenContract imposes constraint that the output is a TokenState`() {
//        ledgerServices.ledger {
//            transaction {
//                // Has wrong output type, will fail.
//                output(TokenContract.ID, DummyState())
//                command(alicePubKey, TokenContract.Issue)
//                fails()
//            }
//            transaction {
//                // Has correct output type, will verify.
//                output(TokenContract.ID, tokenState)
//                command(alicePubKey, TokenContract.Issue)
//                verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `TokenContract imposes constraint that the output TokenState has a positive amount`() {
//        val zeroTokenState = TokenState(alice, bob, -1)
//        val negativeTokenState = TokenState(alice, bob, -1)
//        val positiveTokenState = TokenState(alice, bob, 2)
//
//        ledgerServices.ledger {
//            transaction {
//                // Has zero-amount TokenState, will fail.
//                output(TokenContract.ID, zeroTokenState)
//                command(alicePubKey, TokenContract.Issue)
//                fails()
//            }
//            transaction {
//                // Has negative-amount TokenState, will fail.
//                output(TokenContract.ID, negativeTokenState)
//                command(alicePubKey, TokenContract.Issue)
//                fails()
//            }
//            transaction {
//                // Has positive-amount TokenState, will verify.
//                output(TokenContract.ID, tokenState)
//                command(alicePubKey, TokenContract.Issue)
//                verifies()
//            }
//            transaction {
//                // Also has positive-amount TokenState, will verify.
//                output(TokenContract.ID, positiveTokenState)
//                command(alicePubKey, TokenContract.Issue)
//                verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `TokenContract imposes constraint that the command is an Issue command`() {
//        ledgerServices.ledger {
//            transaction {
//                // Has wrong command type, will fail.
//                output(TokenContract.ID, tokenState)
//                command(alicePubKey, DummyCommandData)
//                fails()
//            }
//            transaction {
//                // Has correct command type, will verify.
//                output(TokenContract.ID, tokenState)
//                command(alicePubKey, TokenContract.Issue)
//                verifies()
//            }
//        }
//    }
//
//    @Test
//    fun `TokenContract imposes constraint that the issuer is a required signer`() {
//        val tokenStateWhereBobIsIssuer = TokenState(bob, alice, 1)
//
//        ledgerServices.ledger {
//            transaction {
//                // Issuer is not a required signer, will fail.
//                output(TokenContract.ID, tokenState)
//                command(bobPubKey, DummyCommandData)
//                fails()
//            }
//            transaction {
//                // Issuer is also not a required signer, will fail.
//                output(TokenContract.ID, tokenStateWhereBobIsIssuer)
//                command(alicePubKey, DummyCommandData)
//                fails()
//            }
//            transaction {
//                // Issuer is a required signer, will verify.
//                output(TokenContract.ID, tokenState)
//                command(alicePubKey, TokenContract.Issue)
//                verifies()
//            }
//            transaction {
//                // Issuer is also a required signer, will verify.
//                output(TokenContract.ID, tokenStateWhereBobIsIssuer)
//                command(bobPubKey, TokenContract.Issue)
//                verifies()
//            }
//        }
//    }
//}