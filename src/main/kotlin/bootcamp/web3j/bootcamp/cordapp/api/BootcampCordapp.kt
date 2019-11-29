package bootcamp.web3j.bootcamp.cordapp.api

import generated.bootcamp.TokenIssueFlowInitiatorPayload
import generated.examples.ArtTransferFlowInitiatorPayload
import javax.annotation.Generated
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import org.web3j.corda.api.CorDapp
import org.web3j.corda.api.Flow
import org.web3j.corda.dapps.LifeCycle
import org.web3j.corda.model.core.transactions.SignedTransaction
import org.web3j.corda.protocol.ClientBuilder
import org.web3j.corda.protocol.CordaException
import org.web3j.corda.protocol.CordaService

/**
 *  CorDapp wrapper.
 */
@Generated(
    value = ["org.web3j.corda.codegen.CorDappClientGenerator"],
    date = "2019-11-27T18:38:08.558Z"
)
@Path("/api/rest/cordapps/bootcamp-cordapp/")
interface BootcampCordapp : CorDapp {

    @get:Path("flows")
    override val flows: FlowResource

    interface FlowResource : org.web3j.corda.api.FlowResource {

        /**
         * Get the TokenIssueFlowInitiator flow.
         */
        @get:Path("bootcamp.TokenIssueFlowInitiator")
        val tokenIssueFlowInitiator: TokenIssueFlowInitiator

        /**
         * BootcampCordapp TokenIssueFlowInitiator flow.
         */
        interface TokenIssueFlowInitiator : Flow {

            /**
             * Start the TokenIssueFlowInitiator flow.
             */
            @POST
            @Produces("application/json")
            @Consumes("application/json")
            fun start(payload: generated.bootcamp.TokenIssueFlowInitiatorPayload): org.web3j.corda.model.core.transactions.SignedTransaction
        }

        /**
         * Get the ArtTransferFlowInitiator flow.
         */
        @get:Path("examples.ArtTransferFlowInitiator")
        val artTransferFlowInitiator: ArtTransferFlowInitiator

        /**
         * BootcampCordapp ArtTransferFlowInitiator flow.
         */
        interface ArtTransferFlowInitiator : Flow {

            /**
             * Start the ArtTransferFlowInitiator flow.
             */
            @POST
            @Consumes("application/json")
            fun start(payload: generated.examples.ArtTransferFlowInitiatorPayload)
        }
    }

    /**
     * BootcampCordapp CorDapp lifecycle methods.
     */
    companion object : LifeCycle<BootcampCordapp> {

        /**
         * Loads an existing BootcampCordapp CorDapp instance.
         */
        override fun load(service: CordaService) = ClientBuilder.build(
            BootcampCordapp::class.java, service, CordaException.Companion::of
        )
    }
}
