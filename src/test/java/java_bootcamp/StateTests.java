package java_bootcamp;

import net.corda.core.contracts.ContractState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class StateTests {
    private final Party alice = new TestIdentity(new CordaX500Name("Alice", "", "GB")).getParty();
    private final Party bob = new TestIdentity(new CordaX500Name("Bob", "", "GB")).getParty();

//    @Test
//    public void tokenStateHasIssuerRecipientAndAmountFieldsOfTheCorrectType() {
//        new TokenState(alice, bob, 1);
//    }

//    @Test
//    public void tokenStateHasGettersForIssuerRecipientAndAmount() {
//        TokenState tokenState = new TokenState(alice, bob, 1);
//        assertEquals(alice, tokenState.getIssuer());
//        assertEquals(bob, tokenState.getRecipient());
//        assertEquals(1, tokenState.getAmount());
//    }

//    @Test
//    public void tokenStateImplementsContractState() {
//        assert(new TokenState(alice, bob, 1) instanceof ContractState);
//    }

//    @Test
//    public void tokenStateHasTwoParticipantsTheIssuerAndTheRecipient() {
//        TokenState tokenState = new TokenState(alice, bob, 1);
//        assertEquals(2, tokenState.getParticipants().size());
//        assert(tokenState.getParticipants().contains(alice));
//        assert(tokenState.getParticipants().contains(bob));
//    }
}