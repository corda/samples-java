# Yo! CorDapp

Send Yo's! to all your friends running Corda nodes!


## Concepts

In the original yo application, the app sent what is essentially a nudge from one endpoint and another.

In corda, we can use abstractions to accomplish the same thing.


We define a state (the yo to be shared), define a contract (the way to make sure the yo is legit), and define the flow (the control flow of our cordapp).

### States
We define a [Yo as a state](https://github.com/corda/samples-java/blob/master/basic-cordapps/yo-cordapp/contracts/src/main/java/net/corda/examples/yo/states/YoState.java#L31-L35), or a corda fact.

```java
    public YoState(Party origin, Party target) {
        this.origin = origin;
        this.target = target;
        this.yo = "Yo!";
    }
```


### Contracts
We define [the "Yo Social Contract"](https://github.com/corda/samples-java/blob/master/basic-cordapps/yo-cordapp/contracts/src/main/java/net/corda/examples/yo/contracts/YoContract.java#L21-L32), which, in this case, verifies some basic assumptions about a Yo.

```java
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<Commands.Send> command = requireSingleCommand(tx.getCommands(), Commands.Send.class);
        requireThat(req -> {
            req.using("There can be no inputs when Yo'ing other parties", tx.getInputs().isEmpty());
            req.using("There must be one output: The Yo!", tx.getOutputs().size() == 1);
            YoState yo = tx.outputsOfType(YoState.class).get(0);
            req.using("No sending Yo's to yourself!", !yo.getTarget().equals(yo.getOrigin()));
            req.using("The Yo! must be signed by the sender.", command.getSigners().contains(yo.getOrigin().getOwningKey()));
            return null;
        });
    }

```


### Flows
And then we send the Yo [within a flow](https://github.com/corda/samples-java/blob/master/basic-cordapps/yo-cordapp/workflows/src/main/java/net/corda/examples/yo/flows/YoFlow.java#L59-L64).

```java
        Party me = getOurIdentity();
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Command<YoContract.Commands.Send> command = new Command<YoContract.Commands.Send>(new YoContract.Commands.Send(), ImmutableList.of(me.getOwningKey()));
        YoState state = new YoState(me, target);
        StateAndContract stateAndContract = new StateAndContract(state, YoContract.ID);
        TransactionBuilder utx = new TransactionBuilder(notary).withItems(stateAndContract, command);
```

On the receiving end, the other corda node will simply receive the Yo using corda provided subroutines, or subflows.

```java
    return subFlow(new ReceiveFinalityFlow(counterpartySession));
```


## Usage


### Pre-Requisites

See https://docs.corda.net/getting-set-up.html.


### Running the nodes

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

Java
``./gradlew deployNodesJava``


then
``./build/nodes/runnodes``

### Sending a Yo

We will interact with the nodes via their specific shells. When the nodes are up and running, use the following command to send a
Yo to another node:

```
    flow start YoFlow target: PartyB
```

Where `NODE_NAME` is 'PartyA' or 'PartyB'. The space after the `:` is required. You are not required to use the full
X500 name in the node shell. Note you can't sent a Yo! to yourself because that's not cool!

To see all the Yo's! other nodes have sent you in your vault (you do not store the Yo's! you send yourself), run:

```
    run vaultQuery contractStateType: net.corda.examples.yo.states.YoState
```
