package bootcamp;

import net.corda.core.contracts.Contract;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.contracts.DummyState;
import net.corda.testing.core.DummyCommandData;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class FinalizeCommandContractTests {
    private final TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private final TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "", "GB"));
    private MockServices ledgerServices = new MockServices(new TestIdentity(new CordaX500Name("TestId", "", "GB")));

    private TokenState tokenState = new TokenState(alice.getParty(), bob.getParty(), 1);

    @Test
    public void finalizeCommandRequiresExactlyOneInputInTheTransaction() {
        transaction(ledgerServices, tx -> {
            // Has no input will fail.
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Finalize());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has two inputs will fail.
            tx.input(TokenContract.ID, tokenState);
            tx.input(TokenContract.ID, tokenState);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Finalize());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has one input, will verify.
            tx.input(TokenContract.ID, tokenState);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Finalize());
            tx.verifies();
            return null;
        });

    }

    @Test
    public void finalizeCommandRequiresZeroOutputsInTheTransaction() {
        transaction(ledgerServices, tx -> {
            // Has one output, will fail.
            tx.input(TokenContract.ID, tokenState);
            tx.output(TokenContract.ID, tokenState);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Finalize());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has zero output, will verify.
            tx.input(TokenContract.ID, tokenState);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Finalize());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void finalizeCommandRequiresOneCommandInTheTransaction() {
        transaction(ledgerServices, tx -> {
            tx.output(TokenContract.ID, tokenState);
            // Has two commands, will fail.
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Issue());
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Issue());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.output(TokenContract.ID, tokenState);
            // Has one command, will verify.
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void finalizeCommandRequiresTheTransactionsInputToBeATokenState() {
        transaction(ledgerServices, tx -> {
            // Has wrong input type, will fail.
            tx.input(TokenContract.ID, new DummyState());
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Finalize());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct input type, will verify.
            tx.input(TokenContract.ID, tokenState);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Finalize());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void finalizeCommandRequiresTheIssuerAndOwnerToBeRequiredSignersInTheTransaction() {

        transaction(ledgerServices, tx -> {
            // Issuer is not a required signer, will fail.
            tx.input(TokenContract.ID, tokenState);
            tx.command(bob.getPublicKey(), new TokenContract.Commands.Finalize());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Owner is not a required signer, will fail.
            tx.input(TokenContract.ID, tokenState);
            tx.command(alice.getPublicKey(), new TokenContract.Commands.Finalize());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Issuer and Owner are required signers, will verify.
            tx.input(TokenContract.ID, tokenState);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new TokenContract.Commands.Finalize());
            tx.verifies();
            return null;
        });
    }
}
