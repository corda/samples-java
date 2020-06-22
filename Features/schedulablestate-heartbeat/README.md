# Heartbeat CorDapp [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Features/schedulablestate-heartbeat)

This CorDapp is a simple showcase of [scheduled activities](https://docs.corda.net/docs/corda-os/event-scheduling.html#how-to-implement-scheduled-events) (i.e. activities started by a node at a specific time without
direct input from the node owner).



## Concepts


### Flows

A node starts its com.heartbeat by calling the `StartHeartbeatFlow`. This creates a `HeartState` on the ledger. This
`HeartState` has a scheduled activity to start the `HeatbeatFlow` one second later.

When the `HeartbeatFlow` runs one second later, it consumes the existing `HeartState` and creates a new `HeartState`.
The new `HeartState` also has a scheduled activity to start the `HeatbeatFlow` in one second.

In this way, calling the `StartHeartbeatFlow` creates an endless chain of `HeartbeatFlow`s one second apart.


You can find those flows here :

 - [StartHeartBeatFlow](./workflows/src/main/java/com/heartbeat/flows/StartHeartbeatFlow.java#L52-L65)

 - [HeartbeatFlow](./workflows/src/main/java/com/heartbeat/flows/HeartbeatFlow.java#L53-L70)



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


### Interacting with the nodes:

Go to the [CRaSH](https://docs.corda.net/docs/corda-os/shell.html) shell for PartyA, and run the `StartHeatbeatFlow`:

    start StartHeartbeatFlow

If you now start monitoring the node's flow activity...

    flow watch

...you will see the `Heartbeat` flow running every second until you close the Flow Watch window using `ctrl/cmd + c`:

    xxxxxxxx-xxxx-xxxx-xx Heartbeat xxxxxxxxxxxxxxxxxxxx Lub-dub
