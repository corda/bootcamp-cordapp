package bootcamp

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.transactions.ValidatingNotaryService
import net.corda.nodeapi.User
import net.corda.nodeapi.internal.ServiceInfo
import net.corda.testing.driver.driver

fun main(args: Array<String>) {
    // No permissions required as we are not invoking flows.
    val user = User("user1", "test", permissions = setOf())
    driver(isDebug = true, startNodesInProcess = true) {
        startNode(providedName = CordaX500Name("NetworkMapAndNotary", "London", "GB"), advertisedServices = setOf(ServiceInfo(ValidatingNotaryService.type)))

        val party = startNode(providedName = CordaX500Name("PartyA", "London", "GB"), rpcUsers = listOf(user)).getOrThrow()
        startWebserver(party)

        waitForAllNodesToFinish()
    }
}