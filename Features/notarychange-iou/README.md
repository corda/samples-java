# IOU - NotaryChange Demo [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Features/notarychange-iou)

This CorDapp serves as a demo of a Notary Change Transaction in Corda which can be performed 
using one of the Corda library flow called `NotaryChangeFlow`.

## Concepts

Notary is a critical component of a Corda network. It helps prevent double-spending 
attempts in a Corda network. Thus all states issued in Corda are tied to a Notary. 
Any transaction involving the update of a state must be notarised from the Notary 
that the state is tied to, since other notaries wouldn't have seen any previous 
transaction involving the state would not be able to prevent a double spending 
attempt.

However, a need to spend a state at a notary other than the one its tied to 
become unavoidable at times. Thus Corda provides `NotaryChangeFlow` to cater to such 
needs.

This demo uses the IOU Demo to demonstrate a Notary Change Transaction. Here we would 
issue an IOU at a particular Notary and try to settle the IOU at a different Notary.


## Usage


### Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

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

If you have any questions during setup, please go to 
https://docs.corda.net/getting-set-up.html for detailed setup instructions.

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