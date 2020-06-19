# blacklist cordapp [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Features/attachment-blacklist)

This CorDapp allows nodes to reach agreement over arbitrary strings of text, but only with parties that are not included in the blacklist uploaded to the nodes as an [attachment](https://training.corda.net/corda-details/attachments/).


## Concepts

The blacklist takes the form of a jar including a single file, `blacklist.txt`. `blacklist.txt` lists the following
parties as being banned from entering into agreements:

* Crossland Savings
* TCF National Bank Wisconsin
* George State Bank
* The James Polk Stone Community Bank
* Tifton Banking Company

The blacklist jar is uploaded as an attachment when building a transaction, and used in the `AgreementContract` to
check that the parties to the `AgreementState` are not blacklisted.

### Flows

There aren't many flows here, so it's quick to cover.

There's a [proposal](./workflows/src/main/java/net/corda/examples/attachments/ProposeFlow.java) and [acceptance](./workflows/src/main/java/net/corda/examples/attachments/AgreeFlow.java) flow, and the blacklist is added as an attachment [here](./workflows/src/main/java/net/corda/examples/attachments/ProposeFlow.java#L47-L50).



## Usage


### Pre-requisites:

See https://docs.corda.net/getting-set-up.html.


### Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```


### Uploading the blacklist:

Note: The nodes must be running before attempting this step

Before attempting to reach any agreements, you must upload the blacklist as an attachment to each node that you want to
be able to *initiate* an agreement. The blacklist can be uploaded via [RPC](https://docs.corda.net/docs/corda-os/api-rpc.html#api-rpc-operations) by running the following command from the
project's root folder:

Java version
* Unix/Mac OSX: ` ./gradlew clients-java:uploadBlacklist`
* Windows: `gradlew clients-java:uploadBlacklist`

Or by running the `Upload blacklist` run configuration from IntelliJ.

You should see three messages of the form `Blacklist uploaded to node via localhost:100XX`.

### Interacting with the nodes:

You can now interact with this CorDapp via the node shell. Note that George State Bank is a blacklisted entity, and the
`AgreementContract` will prevent it from entering into agreements with other nodes.

For example, Monogram Bank and Hiseville Deposit Bank may enter into an agreement by running the following command from
the shell of Monogram Bank:

    start ProposeFlow agreementTxt: "A and B agree Y", counterparty: "Hiseville Deposit Bank", untrustedPartiesAttachment: "4CEC607599723D7E0393EB5F05F24562732CD1B217DEAEDEABD4C25AFE5B333A"

If you now run `run vaultQuery contractStateType: net.corda.examples.attachments.states.AgreementState` on either the
Monogram Bank or Hiseville Deposit Bank node, you should see the agreement stored:

    data: !<net.corda.examples.attachments.state.AgreementState>
      partyA: "O=Monogram Bank, L=London, C=GB"
      partyB: "O=Hiseville Deposit Bank, L=Sao Paulo, C=BR"
      txt: "A and B agree Y"

However, if you try to enter into an agreement with George State Bank from the shell of Monogram Bank:

    start ProposeFlow agreementTxt: "A and B agree Y", counterparty: "George State Bank", untrustedPartiesAttachment: "4CEC607599723D7E0393EB5F05F24562732CD1B217DEAEDEABD4C25AFE5B333A"

The flow will fail and no agreement will be stored!


## Potential Improvements

* Currently, the blacklist jar's hash is hardcoded in the contract. An improvement would be to support any jar signed
  by a specific trusted node
