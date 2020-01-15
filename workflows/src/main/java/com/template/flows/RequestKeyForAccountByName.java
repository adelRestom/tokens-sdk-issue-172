package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccountFlow;
import com.r3.corda.lib.accounts.workflows.flows.SendKeyForAccountFlow;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

import java.util.ArrayList;
import java.util.List;

public class RequestKeyForAccountByName {

    @StartableByRPC
    @InitiatingFlow
    public static class Initiator extends FlowLogic<AnonymousParty> {

        private final Party host;
        private final String name;
        // This flag will be set to true only by IssueTokens flow to create any Finablr Id account that doesn't exist.
        private final boolean createMissingAccount;

        public Initiator(Party host, String name) {
            this.host = host;
            this.name = name;
            this.createMissingAccount = false;
        }

        // This constructor is to be used only by IssueTokens flow.
        public Initiator(Party host, String name, boolean createMissingAccount) {
            this.host = host;
            this.name = name;
            this.createMissingAccount = createMissingAccount;
        }

        @Override
        @Suspendable
        public AnonymousParty call() throws FlowException {
            FlowSession hostSession = initiateFlow(host);
            StateAndRef<AccountInfo> accountStateAndRef;
            // If the account is hosted on the initiating node; we can fetch it with the identity service locally.
            if (host.equals(getOurIdentity())) {
                List<StateAndRef<AccountInfo>> accounts = getServiceHub()
                        .cordaService(KeyManagementBackedAccountService.class).accountInfo(name);
                // Accounts library allows by design to have multiple accounts with the same "name"
                // but different "host"; we need to make sure that we only get the one that is hosted by us.
                accountStateAndRef = accounts.stream()
                        .filter(acc -> acc.getState().getData().getHost().equals(getOurIdentity()))
                        .findAny().orElse(null);
                // If the account is not found.
                if (accountStateAndRef == null) {
                    if (createMissingAccount) {
                        accountStateAndRef = (StateAndRef<AccountInfo>) subFlow(new CreateAccount(name));
                    }
                    else
                        throw new FlowException("Account "+name+" not found.");
                }
            }
            else {
                // Return the account from the host node.
                List<StateAndRef<AccountInfo>> result = hostSession.sendAndReceive(List.class,
                        new FlowParams(name, createMissingAccount)).unwrap(it -> it);
                if (result.size() == 0)
                    throw new FlowException("Account "+name+" not found.");
                accountStateAndRef = result.get(0);
            }
            return subFlow(new RequestKeyForAccountFlow(accountStateAndRef.getState().getData(), hostSession));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void> {

        private final FlowSession otherPartySession;

        public Responder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // No need to do anything if the initiating node is us; we can generate the key locally.
            if (otherPartySession.getCounterparty().equals(getOurIdentity()))
                return null;
            FlowParams params = otherPartySession.receive(FlowParams.class).unwrap(it -> it);
            List<StateAndRef<AccountInfo>> accounts = getServiceHub()
                    .cordaService(KeyManagementBackedAccountService.class).accountInfo(params.name);
            // Accounts library allows by design to have multiple accounts with the same "name"
            // but different "host"; we need to make sure that we only get the one that is hosted by us.
            StateAndRef<AccountInfo> accountStateAndRef = accounts.stream()
                    .filter(acc -> acc.getState().getData().getHost().equals(getOurIdentity()))
                    .findAny().orElse(null);
            List<StateAndRef<AccountInfo>> result = new ArrayList<>();
            if (accountStateAndRef != null)
                result.add(accountStateAndRef);
            else {
                if (params.createMissingAccount) {
                    accountStateAndRef = (StateAndRef<AccountInfo>) subFlow(new CreateAccount(params.name));
                    result.add(accountStateAndRef);
                }
            }
            otherPartySession.send(result);
            subFlow(new SendKeyForAccountFlow(otherPartySession));
            return null;
        }
    }

    @CordaSerializable
    private static class FlowParams {
        private final String name;
        private final boolean createMissingAccount;

        FlowParams(String name, boolean createMissingAccount) {
            this.name = name;
            this.createMissingAccount = createMissingAccount;
        }

        public String getName() {
            return this.name;
        }

        public boolean getCreateMissingAccount() {
            return this.createMissingAccount;
        }
    }
}
