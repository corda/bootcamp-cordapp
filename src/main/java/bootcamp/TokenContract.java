package bootcamp;

import jdk.nashorn.internal.parser.TokenStream;
import liquibase.util.grammar.Token;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;

import java.util.Iterator;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class TokenContract implements Contract {
    public static String ID = "bootcamp.TokenContract";

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {

        CommandWithParties<TokenContract.Commands> command = requireSingleCommand(tx.getCommands(), TokenContract.Commands.class);

        List<ContractState> inputs = tx.getInputStates();
        List<ContractState> outputs = tx.getOutputStates();

        if (command.getValue() instanceof TokenContract.Commands.Issue) {
            requireThat(req -> {
                req.using("Transaction must have no input states.", inputs.isEmpty());
                req.using("Transaction must have exactly one output.", outputs.size() == 1);
                req.using("Output must be a TokenState.", outputs.get(0) instanceof TokenState);
                TokenState output = (TokenState) outputs.get(0);
                req.using("Issuer must be required signer.", command.getSigners().contains(output.getIssuer().getOwningKey()));
                req.using("Owner must be required signer.", command.getSigners().contains(output.getOwner().getOwningKey()));
                req.using("Amount must be positive.", output.getAmount() > 0);
                return null;
            });
        } else if (command.getValue() instanceof TokenContract.Commands.Transfer) {
            requireThat(req -> {
                // input
                req.using("Transaction must have exactly one input state.", inputs.size() == 1);
                req.using("Input must be a TokenState.", inputs.get(0) instanceof TokenState);

                // outputs
                req.using("Transaction must have exactly one OR two output state.", (outputs.size() == 1 || outputs.size() == 2));
                req.using("Output1 must be a TokenState", outputs.get(0) instanceof TokenState);
                TokenState input = (TokenState) inputs.get(0);
                TokenState output1 = (TokenState) outputs.get(0);
                req.using("Input issuer matches Output1 issuer", input.getIssuer().equals(output1.getIssuer()));

                if (outputs.size() == 1) { // there is one output (full value of token is transferred to new owner
                    req.using("Input amount must equal output amount", input.getAmount() == output1.getAmount());
                } else { // there are two outputs (transferred amount, and unspent back to original owner)
                    req.using("Output2 must be a TokenState", outputs.get(1) instanceof  TokenState);
                    TokenState output2 = (TokenState) outputs.get(1);
                    req.using("Input issuer matches Output2 issuer", input.getIssuer().equals(output2.getIssuer()));
                    req.using("Input owner matches Output2 owner", input.getOwner().equals(output2.getOwner()));
                    req.using("Input amount must equal sum of all output amounts",
                            input.getAmount() == (output1.getAmount() + output2.getAmount()));
                }

                // signatures
                req.using("Issuer must be a required signer", command.getSigners().contains(input.getIssuer().getOwningKey()));
                req.using("Current token owner must be a required signer.", command.getSigners().contains(input.getOwner().getOwningKey()));
                req.using("Target/New-Owner must be a required signer.", command.getSigners().contains(output1.getOwner().getOwningKey()));
                return null;
            });
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
    }

    public interface Commands extends CommandData {
        class Issue implements Commands {
        }
        class Transfer implements Commands {
        }
    }
}
