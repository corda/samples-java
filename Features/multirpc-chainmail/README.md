![](./clients/src/main/webapp/src/Components/img/secret_corda.png)

# ChainMail - Corda Multi RPC

This in an implementation of a group messaging client using Corda. It's purpose is to show the capabilities of using MultiRPC for node failover in a High Availability scenario.

This app has a React frontend that allows users to message eachother. Messages are only displayed when all participants have signed and finalised receipt of the message.

## Usage

There's essentially seven processes you'll need to be aware of.

- Five Corda nodes, a notary, and a node for Alice, Bob, Charlie, and David
- The backend webserver that runs the REST endpoints for the corda nodes
- The frontend webserver, a React app that sends requests to the backend.


#### Pre-Requisites

If you've never built a cordapp before you may need to configure gradle and java in order for this code example to run. See [our setup guide](https://docs.corda.net/getting-set-up.html).

### Running these services

#### The five Corda nodes
To run the corda nodes you just need to run the `deployNodes` gradle task and the nodes will be available for you to run directly.

```
./gradlew deployNodes
./build/nodes/runnodes
```

#### The backend webserver

Run the `runChainmailServer` Gradle task.
This will connect using MultiRPC to all the ports defined in the `build.gradle` file inside of the clients directory.
the username `user1` and the password `test`, and serves the webserver on port `localhost:10052` which the React frontend connects to as defined in the `CONSTANTS.js` file.

```
./gradlew runChainMailServer
```
Multi RPC allows for a node to go offline and the server will failover to the next defined node in a round-robin fashion.
With the way the Cordapp is built, this will allow the UI to remain served, however, in order to validate a message, all nodes need to receive a message, so new messages will only appear in the chat once all parties have signed and finalised the transaction.

##### Background Information

`clients/src/main/java/com/chainmail/webserver/` defines a Spring webserver that connects to the nodes via Multi RPC and allows you to interact with the node over HTTP.

The API endpoints are defined in `clients/src/main/java/com/chainmail/webserver/Controller.java`


#### The frontend webserver

The React server can be started by going to `clients/src/main/webapp`, running `npm install` and then `npm start`.

```
cd clients/src/main/webapp
npm install
npm start
```

The frontend will be visible on [localhost:3000](http://localhost:3000)

#### Running tests inside IntelliJ

There are unit tests for the corda state, contract, and tests for both flows used here. You'll find them inside of the various test folders.