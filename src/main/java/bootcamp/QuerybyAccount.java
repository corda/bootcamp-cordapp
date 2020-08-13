package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@StartableByRPC
public class QuerybyAccount extends FlowLogic<List<TokenStateModel>> {

    private final String name;
    public QuerybyAccount(String name) {
        this.name = name;
    }

    @Override
    @Suspendable
    public List<TokenStateModel> call() throws FlowException {
        AccountInfo myAccount = UtilitiesKt.getAccountService(this).accountInfo(name).get(0).getState().getData();
        UUID id = myAccount.getIdentifier().getId();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().withExternalIds(Arrays.asList(id));

        List<StateAndRef<TokenState>> tokenStateAndRefs =  getServiceHub().getVaultService().queryBy(TokenState.class,criteria).getStates();

        if(tokenStateAndRefs.size() == 0){
            return Collections.emptyList();
        }
        return tokenStateAndRefs.stream().map(tokenStateAndRef -> {
            TokenState state = tokenStateAndRef.getState().getData();
            String issuer;
            if(state.getIssuer() instanceof Party)
                issuer = state.getIssuer().toString();
            else
                if(UtilitiesKt.getAccountService(this)
                        .accountInfo(state.getIssuer().getOwningKey()) == null ){
                    issuer = state.getIssuer().toString();
                }
                else{
                    issuer = UtilitiesKt.getAccountService(this)
                            .accountInfo(state.getIssuer().getOwningKey()).getState().getData().getName();
                }

            String owner = UtilitiesKt.getAccountService(this)
                    .accountInfo(state.getOwner().getOwningKey()).getState().getData().getName();
            return new TokenStateModel(issuer, owner, state.getAmount());
        }).collect(Collectors.toList());
    }
}