package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class TokenIssuanceFlowWithSync extends FlowLogic<String> {

    private String issuerAccount;
    private String ownerAccount;
    private int amount;

    public TokenIssuanceFlowWithSync(String issuerAccount, String ownerAccount, int amount) {
        this.issuerAccount = issuerAccount;
        this.ownerAccount = ownerAccount;
        this.amount = amount;
    }

    public TokenIssuanceFlowWithSync(String ownerAccount, int amount) {
        this.ownerAccount = ownerAccount;
        this.amount = amount;
    }

    @Suspendable
    private AnonymousParty getPartyForAccount(String accountName) throws FlowException {
        AccountService accountService = UtilitiesKt.getAccountService(this);
        List<StateAndRef<AccountInfo>> accountList = accountService.accountInfo(accountName);
        if(accountList.size()==0){
            throw new FlowException("Account "+ accountName +" doesn't exist");
        }
        return subFlow(new RequestKeyForAccount(accountList.get(0).getState().getData()));
    }


    @Suspendable
    @Override
    public String call() throws FlowException {

        //grab the notary for transaction building
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //Generate accountinfo & AnonymousParty object for transaction
        AccountService accountService = UtilitiesKt.getAccountService(this);

        AbstractParty issuerParty = issuerAccount == null? getOurIdentity(): getPartyForAccount(issuerAccount);
        AnonymousParty ownerParty = getPartyForAccount(ownerAccount);

        //create a transactionBuilder
        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
        TokenState tokenState = new TokenState(issuerParty, ownerParty , amount);

        transactionBuilder.addOutputState(tokenState);
        transactionBuilder.addCommand(new TokenContract.Commands.Issue() ,
                ImmutableList.of(issuerParty.getOwningKey(), ownerParty.getOwningKey()));

        transactionBuilder.verify(getServiceHub());

        //sign the transaction with the issuer account hosted on the Initiating node
        SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder, issuerParty.getOwningKey());

        FlowSession ownerSession = initiateFlow(accountService.accountInfo(ownerAccount).get(0).getState().getData().getHost());

        //call CollectSignaturesFlow to get the signature from the owner by specifying with issuer key telling CollectSignaturesFlow that issuer has already signed the transaction
        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(selfSignedTransaction, Arrays.asList(ownerSession), Collections.singleton(issuerParty.getOwningKey())));

        SignedTransaction stx;
        //call FinalityFlow for finality
        if(!accountService.accountInfo(ownerAccount).get(0).getState().getData().getHost().equals(getOurIdentity())) {
            stx = subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(ownerSession)));
            try {
                accountService.shareStateAndSyncAccounts(stx.toLedgerTransaction(getServiceHub()).outRef(0) ,ownerSession.getCounterparty());
            } catch (SignatureException e) {
                e.printStackTrace();
            }
        }
        else{
            stx = subFlow(new FinalityFlow(fullySignedTx, Collections.emptyList()));
        }

        return "One Token State issued to "+ownerAccount+ " from " + (issuerAccount==null?issuerParty.toString():issuerAccount)+ " with amount: "+amount +"\ntxId: "+ stx.getId() ;
    }
}


/**
 * This is the responder flow which will get called for the owner to sign the transaction and also to receive the fully signed validated transaction to save in the node's vault and map the token state to
 * owner account
 */
@InitiatedBy(TokenIssuanceFlowWithSync.class)
class TokenIssuanceFlowSyncResponder extends FlowLogic<Void> {

    private final FlowSession otherSide;

    public TokenIssuanceFlowSyncResponder(FlowSession otherSide) {
        this.otherSide = otherSide;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {

        subFlow(new SignTransactionFlow(otherSide) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Owner can add Custom Logic to validate transaction.
            }
        });
        if(!otherSide.getCounterparty().equals(getOurIdentity()))
            subFlow(new ReceiveFinalityFlow(otherSide));

        return null;
    }
}