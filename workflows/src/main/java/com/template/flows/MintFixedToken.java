package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@StartableByRPC
public class MintFixedToken extends FlowLogic<SignedTransaction> {
    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        // My token has 6 fraction digit.
        TokenType token = new TokenType("MyToken", 6);

        // Assign the issuer who will be issuing the tokens.
        IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), token);

        // We will issue 800 tokens of quantity 1.
        Amount<IssuedTokenType> amount = AmountUtilitiesKt.amount(1, issuedTokenType);
        List<FungibleToken> tokens = new ArrayList<>();
        for (int i=0; i<800; i++) {
            //create fungible amount specifying the new owner
            FungibleToken fungibleToken  = new FungibleToken(amount, getOurIdentity(),
                    TransactionUtilitiesKt.getAttachmentIdForGenericParam(token));
            tokens.add(fungibleToken);
        }
        // We will issue another 200 tokens in smaller quantities, so we can have some fractions.
        int sum = 0;
        for (int i=1; i<=78; i++) {
            // Notice here I'm not using "AmountUtilitiesKt.amount".
            Amount<IssuedTokenType> smallerAmount = new Amount(i, issuedTokenType);
            FungibleToken fungibleToken  = new FungibleToken(smallerAmount, getOurIdentity(),
                    TransactionUtilitiesKt.getAttachmentIdForGenericParam(token));
            tokens.add(fungibleToken);
            sum += i;
        }
        for (int i=1; i<=131; i++) {
            // Notice here I'm not using "AmountUtilitiesKt.amount".
            Amount<IssuedTokenType> smallerAmount = new Amount(i * 100, issuedTokenType);
            FungibleToken fungibleToken  = new FungibleToken(smallerAmount, getOurIdentity(),
                    TransactionUtilitiesKt.getAttachmentIdForGenericParam(token));
            tokens.add(fungibleToken);
            sum += i * 100;
        }
        long total = sum + (amount.getQuantity() * 800);

        // You can add a breakpoint here to see the value of "total", which is 800.867681.

        //use built in flow for issuing tokens on ledger
        return subFlow(new IssueTokens(tokens, Collections.emptyList()));
    }
}
