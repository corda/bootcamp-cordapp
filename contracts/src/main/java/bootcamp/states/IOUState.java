package bootcamp.states;

import bootcamp.contracts.IOUContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.List;

// *********
// * State *
// *********
//@BelongsToContract(IOUContract.class)
public class IOUState implements ContractState {

    //private variables


    /* Constructor of your Corda state */

    //getters

    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    public List<AbstractParty> getParticipants() {
        return null;
    }
}