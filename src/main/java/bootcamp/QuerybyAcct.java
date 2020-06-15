package bootcamp;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.flows.StartableByService;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@StartableByRPC
@StartableByService
public class QuerybyAcct extends FlowLogic<List<String>>{

    private final String acctName;

    public QuerybyAcct(String acctname) {
        this.acctName = acctname;
    }

    @Override
    public List<String> call() throws FlowException {

        //Get account Info
        AccountInfo myAccount = UtilitiesKt.getAccountService(this).accountInfo(acctName).get(0).getState().getData();

        //Build account query criteria
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria()
                .withExternalIds(Arrays.asList(myAccount.getIdentifier().getId()));

        //query using account query criteria
        List<String> States = getServiceHub().getVaultService().queryBy(TokenState.class,criteria).getStates().stream().map(
                it -> "\nToken State : " + it.getState().getData()).collect(Collectors.toList());

        return Stream.of(States).flatMap(Collection::stream).collect(Collectors.toList());
    }
}