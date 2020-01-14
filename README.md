Steps for issue: https://github.com/corda/accounts/issues/85

* Deploy the nodes: `./gradlew deployNodes`
* Browse to the nodes (Notary, Mint, and Wallet): `cd build/nodes/`
* Start all nodes (Notary, Mint, and Wallet): `java -jar corda.jar`
* Inside **Mint** terminal Mint tokens: `start MintFixedToken`. This will mint 800.867681.
* Inside **Mint** terminal Issue tokens: `start IssueFixedToken`. This will issue 0.867681 to an **Account123** on Wallet node.
* Open the H2 database: `cd /bin/h2/bin`, `sh h2.sh`, the DB port for Wallet node is 10091

You will see that the account is created inside `accounts` table, and the token is inside `fungible_token` table, but the `account_to_state_refs` table is empty.
The logs didn't show any errors.  
  
<br>
<br>    
  
Initially this repository was for issue https://github.com/corda/token-sdk/issues/172
which I closed, and reopened under Corda repo https://github.com/corda/corda/issues/5853

1. Sadly I wasn't able to replicate the issue.
2. Flow test `fullTest()` passes.
3. I also created a webserver (thinking maybe querying the vault through RPC calls has some issues).
4. I ran the nodes with H2 and Postgres DB's (you can change the settings in root `build.gradle`).
5. To run the API:
   * Deploy the nodes: `./gradlew deployNodes`
   * Browse to the nodes (Notary, Mint, and Wallet): `cd build/nodes/`
   * Start all nodes (Notary, Mint, and Wallet): `java -jar corda.jar`
   * Start the Mint webserver: `./gradlew runTemplateServer`
   * Inside *Mint* terminal Mint tokens: `start MintFixedToken`. This will mint 800.867681.
   * In Postman, call `http://localhost:10050/node-balance`; you should get 800.867681.
   * Inside *Mint* terminal Issue tokens: `start IssueFixedToken`. This will issue 0.867681 to an account on Wallet node.
   * In Postman, call `http://localhost:10050/node-balance`; you should get 800.
   * Inside *Wallet* terminal Revoke tokens: `start RevokeFixedToken`. This will transfer 0.000528 back to the Mint.
   * In Postman, call `http://localhost:10050/node-balance`; you should get 800.000528.
   * Try repeating the last step 20 times, at some point you'll get a different balance (sometimes I can trigger the 
   error in my real code by restarting the webserver in-between calls).
