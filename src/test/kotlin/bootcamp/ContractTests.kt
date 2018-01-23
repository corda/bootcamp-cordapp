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
    fun `integration test`() {
        transaction {
            // Fails without an output state or a command.
            fails()

            // Fails without a command.
            output(SOMETHING_CONTRACT_ID, SomethingState(MEGA_CORP))
            fails()

            tweak {
                // Fails with a command that lists the wrong signer.
                command(MINI_CORP_PUBKEY, SomethingContract.SomethingCommand())
                fails()
            }

            // Succeeds with an output state and a command.
            command(MEGA_CORP_PUBKEY, SomethingContract.SomethingCommand())
            verifies()

            tweak {
                // Fails with an input state.
                input(SOMETHING_CONTRACT_ID, SomethingState(MEGA_CORP))
                fails()
            }

            tweak {
                // Fails with a second output state.
                output(SOMETHING_CONTRACT_ID, SomethingState(MEGA_CORP))
                fails()
            }

            tweak {
                // Fails with a second command.
                command(MEGA_CORP_PUBKEY, SomethingContract.SomethingCommand())
                fails()
            }
        }
    }
}