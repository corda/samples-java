# Ping-Pong CorDapp 
This CorDapp allows a node to ping any other node on the network that also has this CorDapp installed.

It demonstrates how to use Corda for messaging and passing data using a [flow](https://docs.r3.com/en/platform/corda/4.9/community/api-flows.html) without saving any states or using any contracts.


### Concepts


The `ping` utility is normally used to send a Send ICMP ECHO_REQUEST packet to network hosts. The idea being that the receiving host will echo the message back.

We can use corda abstractions to accomplish the same thing.

We define a state (the "ping" to be shared), define a contract (the way to make sure our ping is received correctly), and define the flow (the control flow of our CorDapp).


## Flows

You'll notice in our code we call these two classes ping and pong, the flow that sends the `"ping"`, and the flow that returns with a `"pong"`.


Take a look at [Ping.java](./workflows/src/main/java/net/corda/samples/pingpong/flows/Ping.java).

You'll notice that this flow does what we expect, which is to send an outbound ping, and expect to receive a pong. If we receive a pong, then our flow is successful.

```java
    public Void call() throws FlowException {
        final FlowSession counterpartySession = initiateFlow(counterparty);
        final UntrustworthyData<String> counterpartyData = counterpartySession.sendAndReceive(String.class, "ping");
        counterpartyData.unwrap( msg -> {
            assert(msg.equals("pong"));
            return true;
        });
        return null;
    }
```


And of course we see a similar behavior in [Pong.java](./workflows/src/main/java/net/corda/samples/pingpong/flows/Pong.java).

We expect to receive data from a counterparty that contains a ping, when we receive it, we respond with a pong.

```java
    public Void call() throws FlowException {
        UntrustworthyData<String> counterpartyData = counterpartySession.receive(String.class);
        counterpartyData.unwrap(msg -> {
            assert (msg.equals("ping"));
            return true;
        });
        counterpartySession.send("pong");
        return null;
    }
```


## Pre-Requisites

For development environment setup, please refer to: [Setup Guide](https://docs.r3.com/en/platform/corda/4.9/community/getting-set-up.html).


## Running the nodes


Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean build deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```
## Pinging a node:


### Pinging from the shell
Run the following command from PartyA's shell:
```
start ping counterparty: PartyB
```
Since we are not using any start or transaction, if we want to see the trace of the action. We can look it up from `/build/nodes/PartyA/logs/XXXXX.log` it will be something like:
```
[pool-8-thread-1] shell.FlowShellCommand. - Executing command "flow start ping counterparty: PartyB",
```


### RPC via Gradle:

Run the following command from the root of the project:

* Unix/Mac OSX: `./gradlew pingPartyBJava -Paddress="[your RPC address]" -PnodeName="[name of node to ping]"`
* Windows: `gradlew pingPartyBJava -Paddress="[your RPC address]" -PnodeName="[name of node to ping]"`

For example, if your node has the RPC address `localhost:10006`, you'd ping party B from a
Unix/Mac OSX machine by running:

    ./gradlew pingPartyBJava -Paddress=localhost:10006 -PnodeName="O=PartyB,L=New York,C=US"

You should see the following message, indicating that PartyB responded to your ping:

    Successfully pinged O=PartyB,L=New York,C=US..

### RPC via IntelliJ:

Run the `Run Ping-Pong RPC Client` run configuration from IntelliJ. You can modify the run
configuration to set your node's RPC address and the name of the node to ping.

You should see the following message, indicating that PartyB responded to your ping:

    `Successfully pinged O=PartyB,L=New York,C=US.`.

