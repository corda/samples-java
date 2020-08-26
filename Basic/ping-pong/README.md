# Ping-Pong CorDapp [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Basic/ping-pong)

This CorDapp allows a node to ping any other node on the network that also has this CorDapp installed.

It demonstrates how to use Corda for messaging and passing data using a [flow](https://docs.corda.net/docs/corda-os/flow-state-machines.html#flow-sessions) without saving any states or using any contracts.


### Concepts


The `ping` utility is normally used to send an Send ICMP ECHO_REQUEST packets to network hosts. The idea being that the receiving host will echo the message back.

We can use corda abstractions to accomplish the same thing.

We define a state (the "ping" to be shared), define a contract (the way to make sure our ping is received correctly), and define the flow (the control flow of our cordapp).


## Flows

You'll notice in our code we call these two classes ping and pong, the flow that sends the `"ping"`, and the flow that returns with a `"pong"`.


Take a look at [Ping.java](./workflows-java/src/main/java/net/corda/examples/pingpong/flows/Ping.java#L20-L28).

You'll notice that this flow does what we expect, which is to send an outbound ping, and expect to receive a pong. If we receive a pong, then our flow is sucessful.

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


And of course we see a similar behavior in [Pong.java](./workflows-java/src/main/java/net/corda/examples/pingpong/flows/Pong.java#L22-L30).

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



# Pre-requisites:

See https://docs.corda.net/getting-set-up.html.

# Usage

### Running the CorDapp

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

Java use the `workflows-java:deployNodes` task and `./workflows-java/build/nodes/runnodes` script.
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

