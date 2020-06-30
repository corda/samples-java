# Flow Http CorDapp [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Basic/flow-http-access)

This CorDapp provides a simple example of how HTTP requests can be made in flows. In this case, the flow makes an HTTP
request to retrieve the original BitCoin readme from GitHub.

Be aware that support of HTTP requests in flows is currently limited:

* The request must be executed in a BLOCKING way. Flows don't currently support suspending to await an HTTP call's
  response
* The request must be idempotent. If the flow fails and has to restart from a checkpoint, the request will also be
  replayed



## Concepts


### Flows

Be careful when making HTTP calls in flows; they have to be blocking.
In addition, if the flow fails and is restarted, the HTTP request will be replayed as-is.

You'll find our HTTP request example within [HTTPCallFlow.java](./workflows-java/src/main/java/net/corda/samples/flowhttp/HttpCallFlow.java#L27-L43)

It works mostly as you'd expect, using a request builder to make a request at a client and use the result.

```java
    public String call() throws FlowException {
        final Request httpRequest = new Request.Builder().url(Constants.BITCOIN_README_URL).build();
        String value = null;
        Response httpResponse = null;
        try {
            httpResponse = new OkHttpClient().newCall(httpRequest).execute();
            value = httpResponse.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }

```



## Pre-requisites:

See https://docs.corda.net/getting-set-up.html.

## Usage

### Running the CorDapp

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

Java use the `workflows-java:deployNodes` task and `./workflows-java/build/nodes/runnodes` script.
### Interacting with the nodes:

We'll be interacting with the node via its interactive shell.

To have the node use a flow to retrieve the HTTP of the original Bitcoin URL, run the following command in the node's
shell:

    start HttpCallFlow

The text of the first commit of the BitCoin readme will be printed to the terminal window.
