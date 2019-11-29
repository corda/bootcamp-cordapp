package bootcamp.web3j.bootcamp.cordapp.api

import generated.bootcamp.TokenIssueFlowInitiatorPayload
import generated.examples.ArtTransferFlowInitiatorPayload
import org.junit.jupiter.api.Test
import org.web3j.corda.model.core.identity.Party
import org.web3j.corda.network.network
import org.web3j.corda.network.nodes
import org.web3j.corda.network.notary
import org.web3j.corda.network.party
import javax.annotation.Generated

@Generated(
    value = ["org.web3j.corda.codegen.CorDappClientGenerator"],
    date = "2019-11-27T18:38:08.558Z"
)
class BootcampCordappTest {

    private val network = network {
        nodes {
            notary {
                name = "O=Notary, L=London, C=GB"
            }
            party {
                name = "O=PartyA, L=London, C=GB"
            }
            party {
                name = "O=PartyB, L=New York, C=US"
            }
        }
    }

    private lateinit var generatedBootcampTokenIssueFlowInitiatorPayload: generated.bootcamp.TokenIssueFlowInitiatorPayload
    
    private val partyB: Party by lazy {
        network.parties[1].corda.api.network.nodes.self.legalIdentities.first()
    }

    @Test
    fun `start the TokenIssueFlowInitiator flow`() {
        generatedBootcampTokenIssueFlowInitiatorPayload = TokenIssueFlowInitiatorPayload(partyB, 10)

        BootcampCordapp.load(network.parties[0].corda.service).flows.tokenIssueFlowInitiator
            .start(generatedBootcampTokenIssueFlowInitiatorPayload)
        
        // TODO Add assertions
    }

    private lateinit var generatedExamplesArtTransferFlowInitiatorPayload: generated.examples.ArtTransferFlowInitiatorPayload

    @Test
    fun `start the ArtTransferFlowInitiator flow`() {
        generatedExamplesArtTransferFlowInitiatorPayload = ArtTransferFlowInitiatorPayload(
            "The Scream", "Edvard Munch", partyB
        )

        BootcampCordapp.load(network.parties[0].corda.service).flows.artTransferFlowInitiator
            .start(generatedExamplesArtTransferFlowInitiatorPayload)
        
        // TODO Add assertions
    }
}
