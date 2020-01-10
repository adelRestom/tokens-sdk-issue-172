Initially this was for issue https://github.com/corda/token-sdk/issues/172
which I closed, and reopened under Corda repo https://github.com/corda/corda/issues/5853

1. Sadly I wasn't able to replicate the issue.
2. Flow test `mintTest()` passes.
3. I also created a webserver (thinking maybe querying the vault through RPC calls has some issues).
4. I ran the node with H2 and Postgres DB's (you can change the settings in root `build.gradle`).
5. To run the API:
   * Deploy the node: `./gradlew deployNodes`
   * Browse to the node: `cd buil/nodes/PartyA`
   * Start the node: `java -jar corda.jar`
   * Mint tokens: `start MintFixedToken`
   * Start the webserver: `./gradlew runTemplateServer`
   * Call the API in Postman multiple times (sometimes I can trigger the error in my real code by restarting the webserver
   in-between call): `http://localhost:10050/node-balance`