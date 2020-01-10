package com.template;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import com.template.flows.MintFixedToken;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import net.corda.testing.node.internal.InternalMockNetwork;
import org.junit.Assert;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FlowTests {
    private MockNetwork network ;
    private StartedMockNode walletNode;
    private StartedMockNode mintNode;
    private Party walletParty;
    private Party mintParty;

    @BeforeAll
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.template.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.money"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.selection")
                ))
                // Below values are taken from Tokens SDK ParameterUtilities.testNetworkParameters.
                .withNetworkParameters(new NetworkParameters(4, Collections.EMPTY_LIST,
                        10485760, 10485760*50,
                        Instant.now(), 1, Collections.EMPTY_MAP, Duration.ofDays(30), Collections.EMPTY_MAP)));
        mintNode = network.createPartyNode(
                CordaX500Name.parse("O=Mint,L=London,C=GB"));
        walletNode = network.createPartyNode(
                CordaX500Name.parse("O=Wallet,L=London,C=GB"));
        mintParty = mintNode.getInfo().getLegalIdentities().get(0);
        walletParty = walletNode.getInfo().getLegalIdentities().get(0);

        network.runNetwork();
    }

    @AfterAll
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    @DisplayName("Must be able to mint tokens.")
    public void mintTest() {
        // The quantity is hard coded in our flow.
        // Total = 800,867,681‬
        MintFixedToken flow = new MintFixedToken();
        mintNode.startFlow(flow);
        network.runNetwork();

        // Mint balance must be updated.
        // My token has 6 fraction digit.
        TokenType token = new TokenType("MyToken", 6);


        BigDecimal expectedBalance = BigDecimal.valueOf(800.867681);
        for (int i = 0; i< 100; i++) {
            BigDecimal mintBalance = getNodeBalance(token, mintParty, mintNode);
            Assert.assertEquals(0, mintBalance.compareTo(expectedBalance));
        }

    }

    // Returns the balance in "whole token" numbers.
    // Meaning, if the vault has a token of quantity 1000,0000 and the token has 6 fraction digits;
    // then the returned balance is 1 (1000,000 = 1 whole token).
    private BigDecimal getNodeBalance(TokenType tokenType, Party holder, StartedMockNode holderNode) {

        QueryCriteria queryCriteria = QueryUtilitiesKt.heldTokenAmountCriteria(
                tokenType, holder);

        // Based off the code from Corda documentation (API: Vault Query).
        int pageNumber = 1;
        final int pageSize = 200;
        long totalResults;
        long totalBalance = 0;
        do {
            PageSpecification pageSpec = new PageSpecification(pageNumber, pageSize);
            Vault.Page<FungibleToken> results = holderNode.getServices().getVaultService().queryBy(FungibleToken.class,
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
        BigDecimal fractions = BigDecimal.valueOf(Math.pow(10, tokenType.getFractionDigits()));
        BigDecimal accountBalance = bigTotalBalance.divide(fractions);
        return accountBalance;
    }
}
