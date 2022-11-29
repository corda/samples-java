# Trade Reporting -- ObservableStates

This CorDapp shows how Corda's observable states feature works. Observable states is the ability for nodes who are not
participants in a transaction to still store them if the transactions are sent to them.


## Concepts

In this CorDapp, we assume that when a seller creates some kind of `HighlyRegulatedState`, they must notify the state
and national regulators. There are two ways to use observable states:

* By piggy-backing on `FinalityFlow`
* By distributing the transaction manually

The two approaches are functionally identical.

In this CorDapp, the seller runs the `TradeAndReport` flow to create a new `HighlyRegulatedState`. Then we can see that the seller will:

* Distribute the state to the buyer and the `state regulator` using `FinalityFlow`
* Distribute the state to the `national regulator` manually using the `ReportManually` flow


## Usage

## Pre-Requisites

For development environment setup, please refer to: [Setup Guide](https://docs.r3.com/en/platform/corda/4.9/community/getting-set-up.html).


### Deploy and run the node
```
./greadlew clean build deployNodes
./build/node/runnodes
```

### Interacting with the nodes:

Go to the interactive node shell of Seller, and create a new `HighlyRegulatedState`

    start TradeAndReport buyer: Buyer, stateRegulator: StateRegulator, nationalRegulator: NationalRegulator

The state will be automatically reported to StateRegulator and NationalRegulator, even though they are not
participants. Check this by going to the shell of either node and running:

    run vaultQuery contractStateType: net.corda.samples.observable.states.HighlyRegulatedState

You will see the new `HighlyRegulatedState` in the vault of both nodes.
