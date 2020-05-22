package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class TokenIssuanceFlow extends FlowLogic<String> {

    private final String issuer;
    private final String owner;
    private final int amount;

    public TokenIssuanceFlow(String issuer, String owner, int amount) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {

        //Generate accountinfo & AnonymousParty object for transaction
        AccountInfo issuerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(issuer).get(0).getState().getData();
        AccountInfo ownerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(owner).get(0).getState().getData();

        //Always generate a set of new keys for new transaction
        PublicKey issuerkey = subFlow(new NewKeyForAccount(issuerAccountInfo.getIdentifier().getId())).getOwningKey();//self node
        AnonymousParty ownerAccount = subFlow(new RequestKeyForAccount(ownerAccountInfo));

        //grab the notary for transaction building
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //create the output state
        TokenState tokenState = new TokenState(new AnonymousParty(issuerkey), ownerAccount , amount);

        //create a transactionBuilder
        TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                .addOutputState(tokenState)
                .addCommand(new TokenContract.Commands.Issue(), Arrays.asList(issuerkey,ownerAccount.getOwningKey()));


        //sign the transaction with the signers we got by calling filterMyKeys
        SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder,
                Arrays.asList(getOurIdentity().getOwningKey(),issuerkey));

        //Collect sigs
        FlowSession sessionForAccountToSendTo = initiateFlow(ownerAccountInfo.getHost());
        List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(
                new CollectSignatureFlow(selfSignedTransaction, sessionForAccountToSendTo,ownerAccount.getOwningKey()));
        SignedTransaction fullySignedTx = selfSignedTransaction.withAdditionalSignatures(accountToMoveToSignature);

        //call FinalityFlow for finality
        SignedTransaction stx = subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(sessionForAccountToSendTo)));
        return "One Token State issued to "+owner+ " from " + issuer+ " with amount: "+amount +"\ntxId: "+ stx.getId() ;
    }
}

@InitiatedBy(TokenIssuanceFlow.class)
class TokenIssuanceFlowResponder extends FlowLogic<Void> {
    //private variable
    private FlowSession counterpartySession;

    //Constructor
    public TokenIssuanceFlowResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });
        subFlow(new ReceiveFinalityFlow(counterpartySession));
        return null;
    }
}