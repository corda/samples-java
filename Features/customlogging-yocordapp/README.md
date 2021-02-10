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

When we log this informational message, it gets output along with the other key value pairs we've specified in a JSON formatt: 
```
{
  "instant": {
    "epochSecond": 1612982209,
    "nanoOfSecond": 487000000
  },
  "thread": "Node thread-1",
  "level": "INFO",
  "loggerName": "net.corda",
  "message": "Initializing the transaction.",
  "endOfBatch": true,
  "loggerFqcn": "org.apache.logging.slf4j.Log4jLogger",
  "contextMap": {
    "actor_id": "internalShell",
    "actor_owning_identity": "O=PartyA, L=London, C=GB",
    "actor_store_id": "NODE_CONFIG",
    "fiber-id": "10000001",
    "flow-id": "94543b19-b949-441e-9962-bc50dcd7ad55",
    "initiator": "O=PartyA, L=London, C=GB",
    "invocation_id": "a53a3a5d-b450-456e-a0f1-dfb7dcdce6dd",
    "invocation_timestamp": "2021-02-10T18:36:49.312Z",
    "origin": "internalShell",
    "session_id": "e8ba737e-e809-4a14-8c3b-284b7ae5ed88",
    "session_timestamp": "2021-02-10T18:36:49.022Z",
    "target": "O=PartyB, L=New York, C=US",
    "thread-id": "168"
  },
  "threadId": 168,
  "threadPriority": 5
}
```
This can be quite powerful if you're looking to produce a consumable output stream to a log aggregator like splunk.

You can end up getting log feeds in json that look something like this:

```json
{"instant":{"epochSecond":1612369055,"nanoOfSecond":12000000},"thread":"main","level":"INFO","loggerName":"net.corda.node.internal.Node","message":"Vendor: Corda Open Source","endOfBatch":true,"loggerFqcn":"org.apache.logging.slf4j.Log4jLogger","threadId":1,"threadPriority":5}
. . .More Node Startup loggings

// when our flow is run we see the log we specified
{"instant":{"epochSecond":1612460471,"nanoOfSecond":866000000},"thread":"pool-10-thread-2","level":"INFO","loggerName":"net.corda.tools.shell.FlowShellCommand","message":"Executing command \"flow start net.corda.samples.logging.flows.YoFlow target: PartyA\",","endOfBatch":true,"loggerFqcn":"org.apache.logging.slf4j.Log4jLogger","threadId":224,"threadPriority":5}
{"instant":{"epochSecond":1612460472,"nanoOfSecond":304000000},"thread":"Node thread-1","level":"INFO","loggerName":"net.corda","message":"Initializing the transaction.","endOfBatch":true,"loggerFqcn":"org.apache.logging.slf4j.Log4jLogger","threadId":166,"threadPriority":5}
{"instant":{"epochSecond":1612460472,"nanoOfSecond":428000000},"thread":"pool-10-thread-2","level":"WARN","loggerName":"net.corda.tools.shell.utlities.StdoutANSIProgressRenderer","message":"Cannot find console appender - progre
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

