package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import jdk.nashorn.internal.parser.Token;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@StartableByRPC
public class GetNodeBalance extends FlowLogic<BigDecimal> {

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Override
    @Suspendable
    public BigDecimal call() throws FlowException {
        TokenType token = new TokenType("MyToken", 6);
        QueryCriteria queryCriteria = QueryUtilitiesKt.heldTokenAmountCriteria(
                token, getOurIdentity());

        // Based off the code from Corda documentation (API: Vault Query).
        int pageNumber = 1;
        final int pageSize = 200;
        long totalResults;
        long totalBalance = 0;
        do {
            PageSpecification pageSpec = new PageSpecification(pageNumber, pageSize);
            Vault.Page<FungibleToken> results = getServiceHub().getVaultService().queryBy(FungibleToken.class,
                    queryCriteria, pageSpec);
            totalResults = results.getTotalStatesAvailable();
            if (totalResults == 0)
                return BigDecimal.valueOf(0);
            List<StateAndRef<FungibleToken>> pageMyTokens = results.getStates();
            // Is it possible the below line is behaving randomly?
            long pageBalance = pageMyTokens.stream()
                    .mapToLong(t -> t.getState().getData().getAmount().getQuantity()).sum();
            totalBalance += pageBalance;
            pageNumber++;
        }
        while ((pageSize * (pageNumber - 1) <= totalResults));

        BigDecimal bigTotalBalance = BigDecimal.valueOf(totalBalance);
        // Shoken has 6 fraction digits; meaning 1000,000 = one Shoken.
        BigDecimal fractions = BigDecimal.valueOf(Math.pow(10, token.getFractionDigits()));
        BigDecimal accountBalance = bigTotalBalance.divide(fractions);
        return accountBalance;
    }
}
