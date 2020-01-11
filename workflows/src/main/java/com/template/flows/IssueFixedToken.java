package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@StartableByRPC
public class IssueFixedToken extends FlowLogic<SignedTransaction> {
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

        // Query criteria to fetch only the tokens that belong to the Mint.
        QueryCriteria heldByMint = QueryUtilitiesKt.heldTokenAmountCriteria(
                token, getOurIdentity());

        // Issue 867681 from Mint to account "Account123" on Wallet
        Party walletParty = getServiceHub().getIdentityService()
                .wellKnownPartyFromX500Name(CordaX500Name.parse("O=Wallet,L=London,C=GB"));
        List<PartyAndAmount<TokenType>> partiesAndAmounts = new ArrayList<>();
        AnonymousParty toAccountKey = subFlow(new RequestKeyForAccountByName.Initiator(walletParty,
                "Account123", true));
        Amount<TokenType> amount = new Amount(867681, token);
        PartyAndAmount partyAndAmount = new PartyAndAmount(toAccountKey, amount);
        partiesAndAmounts.add(partyAndAmount);


        return subFlow(new MoveFungibleTokens(partiesAndAmounts, Collections.emptyList(),
                heldByMint, getOurIdentity()));
    }
}
