# Flow Http CorDapp

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

You'll find our HTTP request example within [HTTPCallFlow.java](./workflows/src/main/java/net/corda/samples/flowhttp/HttpCallFlow.java)

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
### Interacting with the nodes:

We'll be interacting with the node via its interactive shell.

To have the node use a flow to retrieve the HTTP of the original Bitcoin URL, run the following command in the node's
shell:

    start HttpCallFlow

The text of the first commit of the BitCoin readme will be printed to the terminal window.
