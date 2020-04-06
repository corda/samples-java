## samples-java/feature-specific-cordapps

This folder features five sample projects, each of them demonstrates different features of corda.  

### [Blacklist](./Attachment-blacklist): 
This CorDapp allows nodes to reach agreement over arbitrary strings of text, but only with parties that are not included in the blacklist uploaded to the nodes.

### [Attachment-sendfile](./Attachment-sendfile): 
This Cordapp shows how to upload and download an attachment via a flow. 

### [Confidential Identity Whistleblower](./ConfidentialIdentity-whistleblower): 
This CorDapp is a simple showcase of confidential identities (i.e. anonymous public keys).

### [Confidential Identity Whistleblower](./ConfidentialIdentity-whistleblower): 
This CorDapp is a simple showcase of confidential identities (i.e. anonymous public keys).

### [Confidential Identity Whistleblower](./ConfidentialIdentity-whistleblower): 
This CorDapp is a simple showcase of confidential identities (i.e. anonymous public keys).
 
### [Service AutoPayroll](./Cordaservice-autopayroll): 
This Cordapp shows how to trigger a flow with vault update(completion of prior flows) using `CordaService` & `trackby`.

### [Observable States Trade Reporting](./ObservableStates-tradereporting): 
This CorDapp shows how Corda's observable states feature works. Observable states is the ability for nodes who are not participants in a transaction to still store them if the transactions are sent to them.

### [Oracle Prime Number](./Oracle-primenumber): 
This CorDapp implements an oracle service that allows nodes to:

* Request the Nth prime number
* Request the oracle's signature to prove that the number included in their transaction is actually the Nth prime number

### [Queryable State Car Insurance](./QueryableState-carinsurance): 
This CorDapp demonstrates QueryableState works in Corda. Corda allows developers to have the ability to expose some or all parts of their states to a custom database table using an ORM tools. To support this feature the state must implement `QueryableState`.

### [Reference States and SanctionsBody](./ReferenceStates-sanctionsBody): 
This CorDapp demonstrates the use of reference states in a transaction and in the verification method of a contract.

This CorDapp allows two nodes to enter into an IOU agreement, but enforces that both parties belong to a list of sanctioned entities. This list of sanctioned entities is taken from a referenced SanctionedEntities state.

### [Heartbeat Schedulable State](./SchedulableState-heartbeat): 
This CorDapp is a simple showcase of scheduled activities (i.e. activities started by a node at a specific time without direct input from the node owner).






