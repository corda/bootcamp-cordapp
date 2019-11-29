package generated.examples

import javax.annotation.Generated
import org.web3j.corda.model.core.identity.Party

/**
*
* @param title
* @param artist
* @param newOwner
*/
@Generated(
    value = ["org.web3j.corda.codegen.CorDappClientGenerator"],
    date = "2019-11-27T18:38:08.503Z"
)
data class ArtTransferFlowInitiatorPayload(
    val title: kotlin.String,
    val artist: kotlin.String,
    val newOwner: org.web3j.corda.model.core.identity.Party
)
