# autopayroll -- cordaservice demo [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Features/cordaservice-autopayroll)

This Cordapp shows how to trigger a flow with vault update(completion of prior flows) using [CordaService](https://training.corda.net/corda-details/automation/#services) & [trackby](https://training.corda.net/corda-details/automation-solution/#track-and-notify).

## Concepts

In this Cordapp, there are four parties:
 - Finance Team: gives payroll order
 - Bank Operater: take the order and automatically initiate the money transfer
 - PetersonThomas: worker #1 will accept money
 - GeorgeJefferson: worker #2 will accept money

There are two states [`PaymentRequestState`](./contracts/src/main/java/net/corda/examples/autopayroll/states/PaymentRequestState.java#L20-L24) & [`MoneyState`](./contracts/src/main/java/net/corda/examples/autopayroll/states/MoneyState.java#L23-L26), and two flows `RequestFlow` & `PaymentFlow`. The business logic looks like the following:
![alt text](./webpic/Business%20Logic.png)

1. Finance team put in payroll request to the bank operators
2. Bank operator receives the requests and process them without stopping


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
if you have any questions during setup, please go to https://docs.corda.net/getting-set-up.html for detailed setup instructions.

Once all four nodes are started up, in Financeteam's node shell, run:
```
flow start RequestFlowInitiator amount: 500, towhom: GeorgeJefferson
```
As a result, we can check for the payment at GeorgeJefferson's node shell by running:
```
run vaultQuery contractStateType: net.corda.examples.autopayroll.states.MoneyState
```
We will see that George Jefferson received a `MoneyState` with amount $500.

Behind the scenes, upon the completion of `RequestFlow`, a request state is stored at Bank operator's vault. The CordaService vault listener picks up the update and calls the `paymentFlow` automatically to send a `moneyState` to the designed reciever.

### Flow triggering using CordaService

The CordaService that triggers the flow is defined in AutoPaymentService.kt. The `CordaService` annotation is used by Corda to find any services that should be created on startup. In order for a flow to be startable by a service, the flow must be annotated with @StartableByService. An example is given in PaymentFlow.kt.
You probably have noticed that `paymentFlow` is not tagged with `@StartableByRPC` like flows normally are. That is, it will not show up in the node shell's flow list. The reason is that `paymentflow` is a completely automated process that does not need any external interactions, so it is ok to be "not-been-seen" from the RPC.

That said, CordaService broadly opens up the probabilities of writing automated flows and fast responding Cordapps!
