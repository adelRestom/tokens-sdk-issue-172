package com.template.webserver;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @GetMapping(value = "/node-balance", produces = MediaType.APPLICATION_JSON_VALUE)
    private ResponseEntity<Map<String, BigDecimal>> getNodeBalance() {
        TokenType token = new TokenType("MyToken", 6);
        QueryCriteria queryCriteria = QueryUtilitiesKt.heldTokenAmountCriteria(
                token, proxy.nodeInfo().getLegalIdentities().get(0));

        // Based off the code from Corda documentation (API: Vault Query).
        int pageNumber = 1;
        final int pageSize = 200;
        long totalResults;
        long totalBalance = 0;
        do {
            PageSpecification pageSpec = new PageSpecification(pageNumber, pageSize);
            Vault.Page<FungibleToken> results = proxy.vaultQueryByWithPagingSpec(FungibleToken.class,
                    queryCriteria, pageSpec);
            totalResults = results.getTotalStatesAvailable();
            if (totalResults == 0)
                return ResponseEntity.ok().body(Collections.singletonMap("Node Balance", BigDecimal.valueOf(0)));
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
        return ResponseEntity.ok().body(Collections.singletonMap("Node Balance", accountBalance));
    }
}