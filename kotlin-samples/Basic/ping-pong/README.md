# Ping-Pong CorDapp

This CorDapp allows a node to ping any other node on the network that also has this CorDapp installed.

It demonstrates how to use Corda for messaging and passing data using a flow that initiates a [communication session](https://docs.r3.com/en/platform/corda/4.9/community/api-flows.html#create-communication-sessions-with-initiateflow) without saving any states or using any contracts.


### Concepts


The `ping` utility is normally used to send an Send ICMP ECHO_REQUEST packets to network hosts. The idea being that the receiving host will echo the message back.

We can use corda abstractions to accomplish the same thing.

We define a state (the "ping" to be shared), define a contract (the way to make sure our ping is received correctly), and define the flow (the control flow of our cordapp).


## Flows

You'll notice in our code we call these two classes ping and pong, the flow that sends the `"ping"`, and the flow that returns with a `"pong"`.


Take a look at initiating flow `PingFlow.kt`.

You'll notice that this flow does what we expect, which is to send an outbound ping, and expect to receive a pong. If we receive a pong, then our flow is sucessful.

```kotlin
    @Suspendable
    override fun call() {
        val counterpartySession = initiateFlow(counterparty)
        val counterpartyData = counterpartySession.sendAndReceive<String>("ping")
        counterpartyData.unwrap { msg ->
            assert(msg == "pong")
        }
    }
```


And of course we see a similar behavior in responder flow `Pong`.

We expect to receive data from a counterparty that contains a ping, when we receive it, we respond with a pong.

```kotlin
    @Suspendable
    override fun call() {
        val counterpartyData = counterpartySession.receive<String>()
        counterpartyData.unwrap { msg ->
            assert(msg == "ping")
        }
        counterpartySession.send("pong")
    }
```



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

* Unix/Mac OSX: `./gradlew pingPartyBKotlin -Paddress="[your RPC address]" -PnodeName="[name of node to ping]"`
* Windows: `gradlew pingPartyBKotlin -Paddress="[your RPC address]" -PnodeName="[name of node to ping]"`

For example, if your node has the RPC address `localhost:10006`, you'd ping party B from a
Unix/Mac OSX machine by running:

    ./gradlew pingPartyBKotlin -Paddress=localhost:10006 -PnodeName="O=PartyB,L=New York,C=US"

You should see the following message, indicating that PartyB responded to your ping:

    Successfully pinged O=PartyB,L=New York,C=US.

