package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

import java.util.List;

@StartableByRPC
@InitiatingFlow
public class ShareAccountFlow extends FlowLogic<String> {

    private final String accountName;
    private final List<Party> recipients;

    public ShareAccountFlow(String accountName, List<Party> recipients) {
        this.accountName = accountName;
        this.recipients = recipients;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        AccountService accountService = UtilitiesKt.getAccountService(this);
        List<StateAndRef<AccountInfo>> accountInfoList = accountService.accountInfo(accountName);
        if(accountInfoList.size()==0){
            throw new FlowException("Account doesn't exist");
        }
        subFlow(new ShareAccountInfo(accountInfoList.get(0), recipients));
        return "" + accountName +" has been shared to " +recipients+".";
    }
}
