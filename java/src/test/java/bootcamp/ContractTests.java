package bootcamp;

import net.corda.core.identity.CordaX500Name;
import net.corda.testing.contracts.DummyState;
import net.corda.testing.core.DummyCommandData;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private final TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "", "GB"));
    private MockServices ledgerServices = new MockServices(new TestIdentity(new CordaX500Name("TestId", "", "GB")));
    private TokenState tokenState = new TokenState(alice.getParty(), bob.getParty(), 1);

    @Test
    public void tokenContractRequiresZeroInputsInTheTransaction() {
        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                // Has an input, will fail.
                tx.input(TokenContract.ID, tokenState);
                tx.output(TokenContract.ID, tokenState);
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.fails();
                return null;
            });

            ledger.transaction(tx -> {
                // Has no input, will verify.
                tx.output(TokenContract.ID, tokenState);
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.verifies();
                return null;
            });

            return null;
        });
    }

    @Test
    public void tokenContractRequiresOneOutputInTheTransaction() {
        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                // Has two outputs, will fail.
                tx.output(TokenContract.ID, tokenState);
                tx.output(TokenContract.ID, tokenState);
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.fails();
                return null;
            });

            ledger.transaction(tx -> {
                // Has one input, will verify.
                tx.output(TokenContract.ID, tokenState);
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.verifies();
                return null;
            });

            return null;
        });
    }

    @Test
    public void tokenContractRequiresOneCommandInTheTransaction() {
        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                tx.output(TokenContract.ID, tokenState);
                // Has two commands, will fail.
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.fails();
                return null;
            });

            ledger.transaction(tx -> {
                tx.output(TokenContract.ID, tokenState);
                // Has one command, will verify.
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.verifies();
                return null;
            });

            return null;
        });
    }

    @Test
    public void tokenContractRequiresTheTransactionsOutputToBeATokenState() {
        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                // Has wrong output type, will fail.
                tx.output(TokenContract.ID, new DummyState());
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.fails();
                return null;
            });

            ledger.transaction(tx -> {
                // Has correct output type, will verify.
                tx.output(TokenContract.ID, tokenState);
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.verifies();
                return null;
            });

            return null;
        });
    }

    @Test
    public void tokenContractRequiresTheTransactionsOutputToHaveAPositiveAmount() {
        TokenState zeroTokenState = new TokenState(alice.getParty(), bob.getParty(), -1);
        TokenState negativeTokenState = new TokenState(alice.getParty(), bob.getParty(), -1);
        TokenState positiveTokenState = new TokenState(alice.getParty(), bob.getParty(), 2);

        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                // Has zero-amount TokenState, will fail.
                tx.output(TokenContract.ID, zeroTokenState);
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.fails();
                return null;
            });

            ledger.transaction(tx -> {
                // Has negative-amount TokenState, will fail.
                tx.output(TokenContract.ID, negativeTokenState);
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.fails();
                return null;
            });

            ledger.transaction(tx -> {
                // Has positive-amount TokenState, will verify.
                tx.output(TokenContract.ID, tokenState);
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.verifies();
                return null;
            });

            ledger.transaction(tx -> {
                // Also has positive-amount TokenState, will verify.
                tx.output(TokenContract.ID, positiveTokenState);
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.verifies();
                return null;
            });

            return null;
        });
    }

    @Test
    public void tokenContractRequiresTheTransactionsCommandToBeAnIssueCommand() {
        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                // Has wrong command type, will fail.
                tx.output(TokenContract.ID, tokenState);
                tx.command(alice.getPublicKey(), DummyCommandData.INSTANCE);
                tx.fails();
                return null;
            });

            ledger.transaction(tx -> {
                // Has correct command type, will verify.
                tx.output(TokenContract.ID, tokenState);
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.verifies();
                return null;
            });

            return null;
        });
    }

    @Test
    public void tokenContractRequiresTheIssuerToBeARequiredSignerInTheTransaction() {
        TokenState tokenStateWhereBobIsIssuer = new TokenState(bob.getParty(), alice.getParty(), 1);

        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                // Issuer is not a required signer, will fail.
                tx.output(TokenContract.ID, tokenState);
                tx.command(bob.getPublicKey(), DummyCommandData.INSTANCE);
                tx.fails();
                return null;
            });

            ledger.transaction(tx -> {
                // Issuer is also not a required signer, will fail.
                tx.output(TokenContract.ID, tokenStateWhereBobIsIssuer);
                tx.command(alice.getPublicKey(), DummyCommandData.INSTANCE);
                tx.fails();
                return null;
            });

            ledger.transaction(tx -> {
                // Issuer is a required signer, will verify.
                tx.output(TokenContract.ID, tokenState);
                tx.command(alice.getPublicKey(), new TokenContract.Issue());
                tx.verifies();
                return null;
            });

            ledger.transaction(tx -> {
                // Issuer is also a required signer, will verify.
                tx.output(TokenContract.ID, tokenStateWhereBobIsIssuer);
                tx.command(bob.getPublicKey(), new TokenContract.Issue());
                tx.verifies();
                return null;
            });

            return null;
        });
    }
}