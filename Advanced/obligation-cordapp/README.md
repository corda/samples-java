# Obligation Cordap

This Cordapp is the complete implementation of our signature IOU (I-owe-you) demonstration.

## Concepts

An IOU is someone who has cash that is paying it back to someone they owe it to. You have to have the original concept of the debt itself, (the IOU), and the cash. Then the ability to exchange assets like cash or assets, and then the ability to settle up. Given this is intended to implement an IOU, our cordapp consists of three flows `issue`, `transfer` and `settle` flows.


### Flows

The first flows are the ones that issue the original cash and assets. You can find that the cash flow at `SelfIssueCashFlow.java` and the IOU issurance in `IOUIssueFlow.java`.

The next flow is the one that transfers ownership of that asset over to another party. That can be found in `IOUTransferFlow.java`.


Finally, once we have the ability to transfer assets, we just need to settle up. That functiionality can be found here in `IOUSettleFlow.java`



## Usage

### Running the CorDapp

Once your application passes all tests in `IOUStateTests`, `IOUIssueTests`, and `IOUIssueFlowTests`, you can run the application and
interact with it via a web browser. To run the finished application, you have two choices for each language: from the terminal, and from IntelliJ.

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```

### Starting the webserver
Once the nodes are up, we will start the webservers next. This app consists of three nodes and one notary, so we will be starting 3 webservers separately. First, lets start PartyA's webserver. Open a new tab of the terminal (make sure you are still in the project directory) and run:
```
./gradlew runPartyAServer
```
repeat the same for PartyB and PartyC, run each of the commands in a new tab:
```
./gradlew runPartyBServer
```
and
```
./gradlew runPartyCServer
```

### Interacting with the CorDapp

Once all the three servers have started up (look for `Webserver started up in XXX sec` in the terminal), you can interact with the app via a web browser.
* From a Node Driver configuration, look for `Starting webserver on address localhost:100XX` for the addresses.

* From the terminal: Node A: `localhost:10009`, Node B: `localhost:10012`, Node C: `localhost:10015`.

To access the front-end gui for each node, navigate to `localhost:XXXX/web/iou/`
