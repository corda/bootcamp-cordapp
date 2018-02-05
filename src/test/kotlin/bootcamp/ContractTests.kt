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
    fun `unwritten test`() {
        TODO()
    }
}