package bootcamp;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TokenTransferFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final Party issuer;
        private final int amount;
        private final Party receiver;

        public Initiator(Party issuer, int amount, Party receiver) {
            this.issuer  = issuer;
            this.amount = amount;
            this.receiver = receiver;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            List<StateAndRef<TokenState>> allTokenStateAndRefs =
                    getServiceHub().getVaultService().queryBy(TokenState.class).getStates();

            AtomicInteger totalTokenAvailable = new AtomicInteger();
            List<StateAndRef<TokenState>> inputStateAndRef = new ArrayList<>();
            AtomicInteger change = new AtomicInteger(0);

            List<StateAndRef<TokenState>> tokenStateAndRefs =  allTokenStateAndRefs.stream()
                    .filter(tokenStateStateAndRef -> {
                        if(tokenStateStateAndRef.getState().getData().getIssuer().equals(issuer)){
                            //Filter inputStates for spending
                            if(totalTokenAvailable.get() < amount)
                                inputStateAndRef.add(tokenStateStateAndRef);

                            //Calculate total tokens available
                            totalTokenAvailable.set(totalTokenAvailable.get() + tokenStateStateAndRef.getState().getData().getAmount());

                            // Determine the change needed to be returned
                            if(change.get() == 0 && totalTokenAvailable.get() > amount){
                                change.set(totalTokenAvailable.get() - amount);
                            }
                            return true;
                        }
                        return false;
                    }).collect(Collectors.toList());

            // Validate if there is sufficient tokens to spend
            if(totalTokenAvailable.get() < amount){
                throw new FlowException("Insufficient balance");
            }

            TokenState outputState = new TokenState( issuer, receiver, amount);

            TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache()
                    .getNotaryIdentities().get(0))
                    .addOutputState(outputState)
                    .addCommand(new TokenContract.Commands.Transfer(), ImmutableList.of(getOurIdentity().getOwningKey()));
            inputStateAndRef.forEach(txBuilder::addInputState);

            if(change.get() > 0){
                TokenState changeState = new TokenState(issuer, getOurIdentity(), change.get());
                txBuilder.addOutputState(changeState);
            }

            txBuilder.verify(getServiceHub());

            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

            // Updated Token State to be send to issuer and receiver
            FlowSession issuerSession = initiateFlow(issuer);
            FlowSession receiverSession = initiateFlow(receiver);

            return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(issuerSession, receiverSession)));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction>{

        private FlowSession otherPartySession;

        public Responder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(otherPartySession));
        }
    }

}



