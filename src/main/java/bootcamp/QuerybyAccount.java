package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@StartableByRPC
public class QuerybyAccount extends FlowLogic<String> {

    private final String whoAmI;
    public QuerybyAccount(String whoAmI) {
        this.whoAmI = whoAmI;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        AccountInfo myAccount = UtilitiesKt.getAccountService(this).accountInfo(whoAmI).get(0).getState().getData();

        UUID id = myAccount.getIdentifier().getId();

        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().withExternalIds(Arrays.asList(id));

        List<StateAndRef<OwnableTokenState>> tokenList = getServiceHub().getVaultService().queryBy(OwnableTokenState.class,criteria).getStates();

        String output = "";

        if(tokenList.size() > 0 ) {
            OwnableTokenState tokenState = tokenList.get(0).getState().getData();
            if(tokenState != null) {

                Party issuerParty = getServiceHub().getIdentityService().wellKnownPartyFromAnonymous(tokenState.getIssuer());
                Party ownerParty = getServiceHub().getIdentityService().wellKnownPartyFromAnonymous(tokenState.getOwner());

                if(issuerParty == null) {
                    output = "Issuer Key to account mapping is not available with this node. Please use SyncKeyMApping or ShareStateWithAccounts flows to sync the mappings";
                    output = output+ " Amount is : " + tokenState.getAmount()
                            + " Owner is : " + UtilitiesKt.getAccountService(this).accountInfo(tokenState.getOwner().getOwningKey()).getState().getData().getName();


                } else if(ownerParty == null) {
                    output = "Owner Key to account mapping is not available with this node. Please use SyncKeyMApping or ShareStateWithAccounts flows to sync the mappings";
                    output = output+ " Amount is : " + tokenState.getAmount()
                            + " Issuer is : " + UtilitiesKt.getAccountService(this).accountInfo(tokenState.getIssuer().getOwningKey()).getState().getData().getName();
                }

                if(issuerParty != null && ownerParty != null) {
                    output = output+ " \nAmount is : " + tokenState.getAmount()
                            + " Issuer is : " + UtilitiesKt.getAccountService(this).accountInfo(tokenState.getIssuer().getOwningKey()).getState().getData().getName()
                            + " Owner is : " + UtilitiesKt.getAccountService(this).accountInfo(tokenState.getOwner().getOwningKey()).getState().getData().getName();
                }
            }

        } else {
            output = "No TokenState mapped to this account on this node. So either this account is not a participant or account to key mapping is not known to this node.Please use SyncKeyMApping or ShareStateWithAccounts flows to sync the mappings";

        }
        return output;
    }
}