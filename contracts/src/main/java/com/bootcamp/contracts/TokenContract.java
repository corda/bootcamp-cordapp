package com.bootcamp.contracts;

import com.bootcamp.states.TokenState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class TokenContract {
    public static String ID = "com.bootcamp.contracts.TokenContract";


    public void verify(LedgerTransaction tx) throws IllegalArgumentException {

    }


    public interface Commands extends CommandData {
        class Issue implements Commands { }
    }
}