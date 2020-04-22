## samples-java/feature-specific-cordapps

This folder features several sample projects, each of them demonstrates different specific features of corda.

### [blacklist](./attachment-blacklist):
This CorDapp allows nodes to reach agreement over arbitrary strings of text, but only with parties that are not included in the blacklist uploaded to the nodes.

### [attachment-sendfile](./attachment-sendfile):
This Cordapp shows how to upload and download an attachment via a flow.

### [confidential identity whistleblower](./confidentialidentity-whistleblower):
This CorDapp is a simple showcase of confidential identities (i.e. anonymous public keys).

### [service autopayroll](./cordaservice-autopayroll):
This Cordapp shows how to trigger a flow with vault update(completion of prior flows) using `CordaService` & `trackby`.

### [observable states trade reporting](./observablestates-tradereporting):
This CorDapp shows how Corda's observable states feature works. Observable states is the ability for nodes who are not participants in a transaction to still store them if the transactions are sent to them.

### [oracle prime number](./oracle-primenumber):
This CorDapp implements an oracle service that allows nodes to:

* Request the Nth prime number
* Request the oracle's signature to prove that the number included in their transaction is actually the Nth prime number

### [queryable state car insurance](./queryablestate-carinsurance):
This CorDapp demonstrates QueryableState works in Corda. Corda allows developers to have the ability to expose some or all parts of their states to a custom database table using an ORM tools. To support this feature the state must implement `QueryableState`.

### [reference states and sanctionsbody](./referencestates-sanctionsbody):
This CorDapp demonstrates the use of reference states in a transaction and in the verification method of a contract.

This CorDapp allows two nodes to enter into an IOU agreement, but enforces that both parties belong to a list of sanctioned entities. This list of sanctioned entities is taken from a referenced SanctionedEntities state.

### [heartbeat schedulable state](./schedulablestate-heartbeat):
This CorDapp is a simple showcase of scheduled activities (i.e. activities started by a node at a specific time without direct input from the node owner).

