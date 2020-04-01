![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)
# Obligation-cordap
This Cordapp is the complete implementation of our signature IOU (I-owe-you) demonstration. 

This cordapps consists of three flows `issue`, `transfer` and `settle` flows. 

## Running the CorDapp
Once your application passes all tests in `IOUStateTests`, `IOUIssueTests`, and `IOUIssueFlowTests`, you can run the application and
interact with it via a web browser. To run the finished application, you have two choices for each language: from the terminal, and from IntelliJ.

### Java
* Terminal: Navigate to the root project folder and run `./gradlew java-source:deployNodes`, followed by
`./java-source/build/node/runnodes`
* IntelliJ: With the project open, select `Java - NodeDriver` from the dropdown run configuration menu, and click
the green play button.

### Interacting with the CorDapp
Once all the three nodes have started up (look for `Webserver started up in XXX sec` in the terminal or IntelliJ ), you can interact
with the app via a web browser.
* From a Node Driver configuration, look for `Starting webserver on address localhost:100XX` for the addresses.

* From the terminal: Node A: `localhost:10009`, Node B: `localhost:10012`, Node C: `localhost:10015`.

To access the front-end gui for each node, navigate to `localhost:XXXX/web/iou/`

