# IOU - NotaryChange 

This CorDapp serves as a demo of a Notary Change Transaction in Corda which can be performed 
using one of the Corda library flow called `NotaryChangeFlow`.

## Concepts

[Notary](https://docs.r3.com/en/platform/corda/4.9/community/key-concepts-notaries.html) is a critical component of a Corda network. It helps prevent double-spending 
attempts in a Corda network. Thus, all states issued in Corda are tied to a Notary. 
Any transaction involving the update of a state must be notarised by the Notary 
that the state is tied to. Since other notaries wouldn't have seen any previous 
transactions involving the state, they would not be able to prevent a double-spending 
attempt.

However, a need to spend a state at a Notary other than the one it's tied to 
can be unavoidable at times. For this reason, Corda provides `NotaryChangeFlow` to cater to such 
needs.

This demo uses an IOU demo to demonstrate a Notary Change Transaction. Here we would 
issue an IOU at a particular Notary and try to settle the IOU using a different Notary.


## Usage



## Pre-Requisites

[Set up for CorDapp development](https://docs.r3.com/en/platform/corda/4.9/community/getting-set-up.html)

### Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the 
nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```
This should bring up 4 nodes (PartyA, PartyB, NotaryA and NotaryB) in 4 different terminals.

To issue an IOU go to PartyA terminal and run:
```
start IssueFlow iouValue: 100, otherParty: PartyB, notary: NotaryA
```

Now, try to settle the IOU at NotaryB. To settle go to PartyB terminal and run:
```
start SettleFlow linearId: <linear id received from issue transaction>, notary: NotaryB
```

The above flow should fail as we are trying to spend the IOU at a notary which is 
different from  the one it was issued. You should see an error message saying:

```
java.lang.IllegalArgumentException: Input state requires notary "O=NotaryA, L=London, C=GB" 
which does not match the transaction notary "O=NotaryB, L=London, C=GB".
```

To run the above settle flow successfully we, first need to do a Notary Change Transaction,
which is done by using the NotaryChangeFlow. Go to PartyB terminal and run:

```
start SwitchNotaryFlow linearId: <linear id received from issue transaction>, newNotary: NotaryB
```

Now, we should be able to settle the IOU at NotaryB successfully. To settle go to PartyB terminal and run:
```
start SettleFlow linearId: <linear id received from issue transaction>, notary: NotaryB
```