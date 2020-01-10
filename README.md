This repository shows an example for this issue: https://github.com/corda/token-sdk/issues/172

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
   * Call API in Postman: `http://localhost:10050/node-balance`