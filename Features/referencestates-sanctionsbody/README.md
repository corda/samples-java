# reference state cordapp [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Features/referencestates-sanctionsbody)

This CorDapp demonstrates the use of [reference states](https://training.corda.net/corda-details/reference-states/) in a transaction and in the verification method of a contract.

## Concepts


This CorDapp allows two nodes to enter into an IOU agreement, but enforces that both parties belong to a list of sanctioned entities. This list of sanctioned entities is taken from a referenced SanctionedEntities state.

### Flows

Next, we want to issue an IOU, this happens in [IOUIssueFlow](./workflows/src/main/java/com.example.flow/IOUIssueFlow.java#L150-L173)


We've seen how to successfully send an IOU to a non-sanctioned party, so what if we want to send one to a sanctioned party? First we need to update the sanction list which you'll find in [UpdateSanctionsListFlow](./workflows/src/main/java/com.example.flow/UpdateSanctionsListFlow.java#L45-L90).


We need to update the reference before we use it in a new transaction, we receive our sanctionslist with the [GetSanctionsListFlow](./workflows/src/main/java/com.example.flow/GetSanctionsListFlow.java#L51-L63)


## Usage


### Pre-Requisites

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

### Running the flows

We will interact with the nodes via their shell.

When the nodes are up and running, the first thing you need to do is create a sanctions list. To do this, open the shell for the SanctionsBody node and run the command:

    flow start IssueSanctionsListFlow

Now that the sanctions list has been made, the party that wants to issue the flow needs to be able to reference it. To do this, it needs to pull the sanction list into its own vault. From the IOUPartyA shell run:

    flow start GetSanctionsListFlow otherParty: SanctionsBody

Next, we want to issue an IOU. Run from the IOUPartyA shell:

    flow start IOUIssueFlow iouValue: 5, otherParty: IOUPartyB, sanctionsBody: SanctionsBody

We've seen how to successfully send an IOU to a non-sanctioned party, so what if we want to send one to a sanctioned party? First we need to update the sanction list so, from the SanctionsParty shell, run:

    flow start UpdateSanctionsListFlow partyToSanction: DodgyParty

We need to update the reference before we use it in a new transaction so, from IOUPartyA's shell, run:

    flow start GetSanctionsListFlow otherParty: SanctionsBody

Now try an issue a flow to DodgyParty:

    flow start IOUIssueFlow iouValue: 5, otherParty: DodgyParty, sanctionsBody: SanctionsBody

The flow will error with the message 'java.lang.IllegalArgumentException: Failed requirement: The borrower O=DodgyParty, L=Moscow, C=RU is a sanctioned entity'!

