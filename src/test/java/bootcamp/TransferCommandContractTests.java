package bootcamp;

import net.corda.core.identity.CordaX500Name;
import net.corda.testing.contracts.DummyState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class TransferCommandContractTests {
    private final TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private final TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "", "GB"));
    private final TestIdentity john = new TestIdentity(new CordaX500Name("John", "", "GB"));
    private MockServices ledgerServices = new MockServices(new TestIdentity(new CordaX500Name("TestId", "", "GB")));

    private TokenState tsInputAmount1 = new TokenState(alice.getParty(), bob.getParty(), 1);
    private TokenState tsInputAmount2 = new TokenState(alice.getParty(), bob.getParty(), 2);
    private TokenState tsOutputXfer = new TokenState(alice.getParty(), john.getParty(), 1); // new owner
    private TokenState tsOutputRemain = new TokenState(alice.getParty(), bob.getParty(), 1);

    @Test
    public void transferCommandRequiresOneInputInTheTransaction() {
        transaction(ledgerServices, tx -> {
            // Has no input, will fail.
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has two inputs, will fail.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has ONE input, will verify.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void transferCommandRequiresTheTransactionsInputToBeATokenState() {
        transaction(ledgerServices, tx -> {
            // Has wrong input type, will fail.
            tx.input(TokenContract.ID, new DummyState());
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct input type, will verify.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void transferCommandRequiresOneOrTwoOutputs() {
        transaction(ledgerServices, tx -> {
            // Has zero outputs, will fail.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has three outputs, will fail.
            tx.input(TokenContract.ID, new TokenState(alice.getParty(), bob.getParty(), 3));
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has one output, will verify.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has two outputs, will verify.
            tx.input(TokenContract.ID, tsInputAmount2);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.output(TokenContract.ID, tsOutputRemain);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void transferCommandRequiresTheTransactionsOutputsToBeATokenState() {
        transaction(ledgerServices, tx -> {
            // Has wrong output type single output, will fail.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, new DummyState());
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct output type single output, will verify.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has wrong output types for two outputs, will fail.
            tx.input(TokenContract.ID, tsInputAmount2);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.output(TokenContract.ID, new DummyState());
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct output types for two outputs, will verify.
            tx.input(TokenContract.ID, tsInputAmount2);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.output(TokenContract.ID, tsOutputRemain);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void transferCommandRequiresOutputIssuerToMatchInputIssuer() {
        transaction(ledgerServices, tx -> {
            // Issuer doesn't match for one output will fail.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, new TokenState(bob.getParty(), john.getParty(), 1));
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct issuer for one output will verify.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Issuer doesn't match for two outputs will fail.
            tx.input(TokenContract.ID, tsInputAmount2);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.output(TokenContract.ID, new TokenState(john.getParty(), bob.getParty(), 1));
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct issuer for two outputs will pass.
            tx.input(TokenContract.ID, tsInputAmount2);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.output(TokenContract.ID, tsOutputRemain);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void transferCommandRequiresInputAmountToEqualOutput() {
        transaction(ledgerServices, tx -> {
            // Has wrong amount for single output, fails. (1 != 3)
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, new TokenState(bob.getParty(), john.getParty(), 3));
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct amount for single output, verifies. (1 = 1)
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has wrong amount for two outputs, fails. (1 != 2)
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.output(TokenContract.ID, tsOutputRemain);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct amount for two outputs, verifies. (2 = 2)
            tx.input(TokenContract.ID, tsInputAmount2);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.output(TokenContract.ID, tsOutputRemain);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void transferCommandRequiresSecondOutputOwnerMatchInputOwner() {
        // in the case full value isn't transferred then remainder is assigned to original owner
        transaction(ledgerServices, tx -> {
            // Both outputs owned by target, will fail.
            tx.input(TokenContract.ID, tsInputAmount2);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Second output owned by original owner, will verify.
            tx.input(TokenContract.ID, tsInputAmount2);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.output(TokenContract.ID, tsOutputRemain);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void transferCommandRequiresCorrectSignatures() {

        // ISSUER MUST SIGN
        transaction(ledgerServices, tx -> {
            // No issuer signature, will fail.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        // ORIGINAL OWNER MUST SIGN
        transaction(ledgerServices, tx -> {
            // No owner signature, will fail.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        // TARGET/NEW OWNER MUST SIGN
        transaction(ledgerServices, tx -> {
            // No target signature, will fail.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.fails();
            return null;
        });

        // ALL SIGNATURES PRESENT
        transaction(ledgerServices, tx -> {
            // Issuer, Owner, and Target signatures, will verify.
            tx.input(TokenContract.ID, tsInputAmount1);
            tx.output(TokenContract.ID, tsOutputXfer);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey(), john.getPublicKey()), new TokenContract.Commands.Transfer());
            tx.verifies();
            return null;
        });
    }
}
