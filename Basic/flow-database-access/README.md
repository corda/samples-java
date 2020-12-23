# Flow Database Access CorDapp 
This CorDapp provides a simple example of how the node database can be accessed in flows using a [JDBC Connection](https://docs.corda.net/docs/corda-os/api-persistence.html#jdbc-session). In this case, the flows
maintain a table of cryptocurrency values in the node's database.


## Concepts

### Flows

The CorDapp defines three flows: `AddTokenValueFlow`, `UpdateTokenValueFlow`, and `QueryTokenValueFlow`. Under the hood, the database accesses are managed by the CryptoValuesDatabaseService [CordaService](https://training.corda.net/corda-details/automation/#services).

Be aware that support of database accesses in flows is currently limited:

* The operation must be executed in a BLOCKING way. Flows don't currently support suspending to await an operation's response
* The operation must be idempotent. If the flow fails and has to restart from a checkpoint, the operation will also be replayed


## Pre-Requisites

For development environment setup, please refer to: [Setup Guide](https://docs.corda.net/getting-set-up.html).


## Running the nodes


Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```

### Interacting with the node:

We'll be interacting with the node via its interactive shell.

Suppose we want to add a token called `mango_coin` to the node's database table with an initial value of 100. In the
node's interactive shell, run the following command:

    start AddTokenValueFlow token: "mango_coin", value: 100

And read back `mango_coin`'s value from the node's database table by running:

    start QueryTokenValueFlow token: "mango_coin"

We can then update `mango_coin`'s value to 500 by running:

    start UpdateTokenValueFlow token: "mango_coin", value: 500

Again read back `mango_coin`'s value from the node's database table by running:

    start QueryTokenValueFlow token: "mango_coin"
