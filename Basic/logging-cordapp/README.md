# Logging CorDapp

## Custom Logging

This is a modified version of the original yo cordapp with some additions to use custom log4j2 configurations.


The primary example we've implemented here is json logging which is configured in `config/dev/log4j2.xml`.

This gives us the ability to use Log4j thread contexts to log arbitrary objects or data points in json format.

In this example not only do the node logs output in json but we can add arbitrary key value pairs as well.

```java
    // here we have our first opportunity to log out the contents of the flow arguments.
    ThreadContext.put("initiator", me.getName().toString());
    ThreadContext.put("target", target.getName().toString());
    // publish to the log with the additional context
    logger.info("Initializing the transaction.");
```

When we log this informational message, it gets output along with the other key value pairs we've specified.
This can be quite powerful if you're looking to produce a consumable output stream to a log aggregator like splunk.

You can end up getting log feeds in json that look something like this:

```json
{"instant":{"epochSecond":1612369055,"nanoOfSecond":12000000},"thread":"main","level":"INFO","loggerName":"net.corda.node.internal.Node","message":"Vendor: Corda Open Source","endOfBatch":true,"loggerFqcn":"org.apache.logging.slf4j.Log4jLogger","threadId":1,"threadPriority":5}
{"instant":{"epochSecond":1612369055,"nanoOfSecond":12000000},"thread":"main","level":"INFO","loggerName":"net.corda.node.internal.Node","message":"Release: 4.6","endOfBatch":false,"loggerFqcn":"org.apache.logging.slf4j.Log4jLogger","threadId":1,"threadPriority":5}
{"instant":{"epochSecond":1612369055,"nanoOfSecond":12000000},"thread":"main","level":"INFO","loggerName":"net.corda.node.internal.Node","message":"Platform Version: 8","endOfBatch":false,"loggerFqcn":"org.apache.logging.slf4j.Log4jLogger","threadId":1,"threadPriority":5}
{"instant":{"epochSecond":1612369055,"nanoOfSecond":12000000},"thread":"main","level":"INFO","loggerName":"net.corda.node.internal.Node","message":"Revision: 85e387ea730d9be7d6dc2b23caba1ee18305af74","endOfBatch":false,"loggerFqcn":"org.apache.logging.slf4j.Log4jLogger","threadId":1,"threadPriority":5}
{"instant":{"epochSecond":1612369055,"nanoOfSecond":13000000},"thread":"main","level":"INFO","loggerName":"net.corda.node.internal.Node","message":"PID: 94369","endOfBatch":false,"loggerFqcn":"org.apache.logging.slf4j.Log4jLogger","threadId":1,"threadPriority":5}
. . .


// when our flow is run we see the log we specified
{"instant":{"epochSecond":1612460471,"nanoOfSecond":866000000},"thread":"pool-10-thread-2","level":"INFO","loggerName":"net.corda.tools.shell.FlowShellCommand","message":"Executing command \"flow start net.corda.samples.logging.flows.YoFlow target: PartyA\",","endOfBatch":true,"loggerFqcn":"org.apache.logging.slf4j.Log4jLogger","threadId":224,"threadPriority":5}
{"instant":{"epochSecond":1612460472,"nanoOfSecond":304000000},"thread":"Node thread-1","level":"INFO","loggerName":"net.corda","message":"Initializing the transaction.","endOfBatch":true,"loggerFqcn":"org.apache.logging.slf4j.Log4jLogger","threadId":166,"threadPriority":5}
{"instant":{"epochSecond":1612460472,"nanoOfSecond":428000000},"thread":"pool-10-thread-2","level":"WARN","loggerName":"net.corda.tools.shell.utlities.StdoutANSIProgressRenderer","message":"Cannot find console appender - progre
```


## Concepts

In the original yo application that this sample was based on, the app sent what is essentially a "yo" state from one node to another.

In corda, we can use abstractions to accomplish the same thing.

If you're not interested in how the cordapp works and want to see the logging, feel free to skip down to the usage section.


We define a [state](https://training.corda.net/key-concepts/concepts/#states) (the yo to be shared), define a [contract](https://training.corda.net/key-concepts/concepts/#contracts) (the way to make sure the yo is legit), and define the [flow](https://training.corda.net/key-concepts/concepts/#flows) (the control flow of our cordapp).

### States
We define a [Yo as a state](./contracts/src/main/java/net/corda/examples/yo/states/YoState.java#L31-L35), or a corda fact.

```java
    public YoState(Party origin, Party target) {
        this.origin = origin;
        this.target = target;
        this.yo = "Yo!";
    }
```


### Contracts
We define the ["Yo Social Contract"](./contracts/src/main/java/net/corda/examples/yo/contracts/YoContract.java#L21-L32), which, in this case, verifies some basic assumptions about a Yo.

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
And then we send the Yo [within a flow](./workflows/src/main/java/net/corda/examples/yo/flows/YoFlow.java#L59-L64).

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


### Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)

```
./build/nodes/runnodes
```

When the nodes run you'll see the log entries in json on STDOUT, and you'll also be able to see the node's json log files in each folder.

```shell
cat ./build/nodes/PartyA/logs/node.json

{"instant":{"epochSecond":1612543764,"nanoOfSecond":930000000},"thread":"main","level":"INFO","loggerName":"net.corda.cliutils.CliWrapperBase","message":"Application Args: run-migration-scripts --core-schemas --app-schemas","endOfBatch":true,"loggerFqcn":"org.apache.logging.slf4j.Log4jLogger","contextMap":{},"threadId":1,"threadPriority":5}
{"instant":{"epochSecond":1612543766,"nanoOfSecond":300000000}

. . .
```

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
    run vaultQuery contractStateType: YoState
```

### Viewing custom logs

This will depend on your cordapp setup, if you're running your corda nodes all you need to do is specify the particular config file.

You can do that in a couple of ways:

 - You can always just run the jar directly

```shell
java -Dlog4j.configurationFile=logging-cordapp/build/resources/main/log4j2.xml -jar corda.jar
```


- Or if you're running with the bootstrapped corda network you can add this argument to the result of the runnodes command.

> notice that all we're doing is adding this param to the command we'd otherwise use to run corda in order to specify the log file.
> When you normally run the node bootstrapper on localhost you'll see that it will generate a command that looks like this for each node on your network. All you'd need to do is add the log4j configurationFile flag to that startup command to have access to these logs in your node as well.

```
'cd "/Users/corda/logging-cordapp/build/nodes/PartyA" ; "/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/bin/java" "-Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006 -javaagent:drivers/jolokia-jvm-1.6.0-agent.jar=port=7006,logHandlerClass=net.corda.node.JolokiaSlf4jAdapter" "-Dname=PartyA" "-jar" "-Dlog4j.configurationFile=/Users/corda/logging-cordapp/build/resources/main/log4j2.xml" "/Users/corda/logging-cordapp/build/nodes/PartyA/corda.jar" ; and exit'
```



## Attribution

This example was built in collaboration with [Splunk](https://splunk.com), and they have our thanks.





