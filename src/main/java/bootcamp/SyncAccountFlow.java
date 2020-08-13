package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.ShareStateAndSyncAccounts;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;

import java.util.Arrays;
import java.util.List;

@StartableByRPC
@InitiatingFlow
public class SyncAccountFlow extends FlowLogic<String> {

    private String account;
    private Party syncWith;

    public SyncAccountFlow(String account, Party syncWith) {
        this.account = account;
        this.syncWith = syncWith;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        AccountInfo accountInfo = UtilitiesKt.getAccountService(this).accountInfo(account).get(0).getState().getData();
        QueryCriteria.VaultQueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria()
                .withExternalIds(Arrays.asList(accountInfo.getIdentifier().getId())).withStatus(Vault.StateStatus.UNCONSUMED);
        try {
            List<StateAndRef<TokenState>> tokenStateAndRefList = getServiceHub().getVaultService().queryBy(TokenState.class,queryCriteria).getStates();
            for(StateAndRef<TokenState> tokenStateStateAndRef: tokenStateAndRefList){
                subFlow(new ShareStateAndSyncAccounts(tokenStateStateAndRef, syncWith));
            }

        }catch (Exception e){
            throw new FlowException("Account not found");
        }

        return "Accounts Synced Successfully!";
    }
}
