# Auto Payroll -- CordaService

This CorDapp shows how to trigger a flow with vault update(completion of prior flows) using [CordaService](https://training.corda.net/corda-details/automation/#services) & [trackby](https://training.corda.net/corda-details/automation-solution/#track-and-notify).

## Concepts

In this CorDapp, there are four parties:
 - Finance Team: gives payroll order
 - Bank Operator: takes the order and automatically initiates the money transfer
 - PetersonThomas: worker #1 who accepts the money
 - GeorgeJefferson: worker #2 who accepts the money

There are two states `PaymentRequestState` and `MoneyState`, and two flows `RequestFlow` & `PaymentFlow`. The business logic looks like the following:
![alt text](./webpic/Business%20Logic.png)

1. Finance team puts in payroll request to the bank operators
2. Bank operator receives the requests and processes them without stopping


## Usage

## Pre-Requisites

[Set up for CorDapp development](https://docs.r3.com/en/platform/corda/4.9/community/getting-set-up.html)

### Deploy and run the nodes
```
./gradlew clean build deployNodes
./build/node/runnodes
```

Once all four nodes are started up, in Finance Team's node shell, run:
```
flow start RequestFlowInitiator amount: 500, towhom: GeorgeJefferson
```
As a result, we can check for the payment in GeorgeJefferson's node shell by running:
```
run vaultQuery contractStateType: net.corda.samples.autopayroll.states.MoneyState
```
We will see that George Jefferson received a `MoneyState` with amount $500.

Behind the scenes, upon the completion of `RequestFlow`, a request state is stored at Bank operator's vault. The CordaService vault listener picks up the update and calls the `paymentFlow` automatically to send a `moneyState` to the designated receiver.

### Flow triggering using CordaService

The CordaService that triggers the flow is defined in `AutoPaymentService.kt`. The `CordaService` annotation is used by Corda to find any services that should be created on startup. In order for a flow to be startable by a service, the flow must be annotated with `@StartableByService`. 

An example is given in `PaymentFlow.kt`. You probably have noticed that `paymentFlow` is not tagged with `@StartableByRPC` like flows normally are. That means it will not show up in the node shell's flow list. The reason for that is because `paymentflow` is a completely automated process that does not need any external interactions, so it is ok to be "not-been-seen" from the RPC.

That said, CordaService broadly opens up the probabilities of writing automated flows and fast responding CorDapps!
