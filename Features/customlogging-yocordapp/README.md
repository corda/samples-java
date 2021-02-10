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
Here we're highlighting how easy it is to issue log messages with arbitrary key value pairs, we have an example of this in our YoFlow.

```java
public SignedTransaction call() throws FlowException {
    // note we're creating a logger first with the shared name from our other example.
    Logger logger = LoggerFactory.getLogger("net.corda");

    progressTracker.setCurrentStep(CREATING);

    Party me = getOurIdentity();

    // here we have our first opportunity to log out the contents of the flow arguments.
    ThreadContext.put("initiator", me.getName().toString());
    ThreadContext.put("target", target.getName().toString());
    // publish to the log with the additional context
    logger.info("Initializing the transaction.");
    // flush the threadContext
    ThreadContext.removeAll(Arrays.asList("initiator", "target"));

    // Obtain a reference to a notary.
    final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

    Command<YoContract.Commands.Send> command = new Command<YoContract.Commands.Send>(new YoContract.Commands.Send(), Arrays.asList(me.getOwningKey()));
    YoState state = new YoState(me, target);
    StateAndContract stateAndContract = new StateAndContract(state, YoContract.ID);
    TransactionBuilder utx = new TransactionBuilder(notary).withItems(stateAndContract, command);

    progressTracker.setCurrentStep(VERIFYING);
    utx.verify(getServiceHub());

    progressTracker.setCurrentStep(SIGNING);
    SignedTransaction stx = getServiceHub().signInitialTransaction(utx);

    // inject details to the threadcontext to be exported as json
    ThreadContext.put("tx_id", stx.getId().toString());
    ThreadContext.put("notary", notary.getName().toString());
    // publish to the log with the additional context
    logger.info("Finalizing the transaction.");
    // flush the threadContext
    ThreadContext.removeAll(Arrays.asList("tx_id", "notary"));

    progressTracker.setCurrentStep(FINALISING);
    FlowSession targetSession = initiateFlow(target);
    return subFlow(new FinalityFlow(stx, Arrays.asList(targetSession), Objects.requireNonNull(FINALISING.childProgressTracker())));
}
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

When the nodes run you'll be able to see the node's json log files in their respesctive `logs` folders.
This logging configuration will add a new file that you can view.

```shell
tail -f  build/nodes/PartyA/logs/node.json

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

### Other ways to use this log configuration

The above method will run all nodes together but if you're running your corda node manually all you need to do is specify the particular config file.

You can do that by just running the jar directly:

```shell
java -Dlog4j.configurationFile=logging-cordapp/build/resources/main/log4j2.xml -jar corda.jar
```

> notice that all we're doing is adding this param to the command we'd otherwise use to run corda in order to specify the log file.


## Attribution

This example was built with help from [Splunk](https://splunk.com), and they have our thanks.

