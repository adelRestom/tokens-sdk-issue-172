package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@StartableByRPC
public class RevokeFixedToken extends FlowLogic<SignedTransaction> {
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

        // Query criteria to fetch only the tokens that belong to "Account123".
        List<StateAndRef<AccountInfo>> results = this.getServiceHub()
                .cordaService(KeyManagementBackedAccountService.class).accountInfo("Account123");
        if (results.size() == 0)
            throw new FlowException("Account Account123 not found.");
        StateAndRef<AccountInfo> fromAccountResult = results.get(0);
        // Query criteria to fetch only the tokens that belong to the source account.
        QueryCriteria heldByFromAccount = new QueryCriteria.VaultQueryCriteria(
                Vault.StateStatus.UNCONSUMED,
                Collections.singleton(FungibleToken.class),
                null,
                null,
                null,
                null,
                Vault.RelevancyStatus.ALL,
                Collections.emptySet(),
                Collections.emptySet(),
                null,
                Collections.singletonList(fromAccountResult.getState().getData().getIdentifier().getId()));

        // Revoke 867681 from Account123 back to Mint
        Party mintParty = getServiceHub().getIdentityService()
                .wellKnownPartyFromX500Name(CordaX500Name.parse("O=Mint,L=London,C=GB"));
        AnonymousParty fromAccount = subFlow(new RequestKeyForAccountByName.Initiator(getOurIdentity(),
                "Account123"));

        List<PartyAndAmount<TokenType>> partiesAndAmounts = new ArrayList<>();

        // Revoke a total of 528
        int sum = 0;
        for (int i=1; i<=32; i++) {
            // Notice here I'm not using "AmountUtilitiesKt.amount".
            Amount<IssuedTokenType> smallerAmount = new Amount(i, token);
            PartyAndAmount partyAndAmount = new PartyAndAmount(mintParty, smallerAmount);
            partiesAndAmounts.add(partyAndAmount);
            sum += i;
        }

        return subFlow(new MoveFungibleTokens(partiesAndAmounts,
                Collections.emptyList(),
                heldByFromAccount, fromAccount));
    }
}
