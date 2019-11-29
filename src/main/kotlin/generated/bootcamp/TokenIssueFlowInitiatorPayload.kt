package generated.bootcamp

import javax.annotation.Generated
import org.web3j.corda.model.core.identity.Party

/**
*
* @param owner
* @param amount
*/
@Generated(
    value = ["org.web3j.corda.codegen.CorDappClientGenerator"],
    date = "2019-11-27T18:38:08.503Z"
)
data class TokenIssueFlowInitiatorPayload(
    val owner: org.web3j.corda.model.core.identity.Party,
    val amount: kotlin.Int
)
