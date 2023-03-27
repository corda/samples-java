# Whistleblower -- Confidential Identity 

This CorDapp is a simple showcase of [confidential identities](https://docs.r3.com/en/platform/corda/4.9/community/api-identity.html#confidential-identities) (i.e. anonymous public keys).

## Concepts


A node (the *whistle-blower*) can whistle-blow on a company to another node (the *investigator*). Both the
whistle-blower and the investigator generate anonymous public keys for this transaction, meaning that any third-parties
who manage to get a hold of the state cannot identify the whistle-blower or investigator. This process is handled
automatically by the `SwapIdentitiesFlow`.

## Usage


## Pre-Requisites

[Set up for CorDapp development](https://docs.r3.com/en/platform/corda/4.9/community/getting-set-up.html)


### Deploy and run the node
Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean build deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```

### Interacting with the nodes:

We will interact with this CorDapp via the nodes' interactive shells.

First, go to the shell of BraveEmployee, and report BadCompany to the TradeBody by running:

    flow start BlowWhistleFlow badCompany: BadCompany, investigator: TradeBody

To see the whistle-blowing case stored on the BraveEmployee node, run:

    run vaultQuery contractStateType: net.corda.samples.whistleblower.states.BlowWhistleState

You should see something similar to the following:

    [ {
      "badCompany" : "C=KE,L=Eldoret,O=BadCompany",
      "whistleBlower" : "8Kqd4oWdx4KQGHGKubAvzAFiUG2JjhHxM2chUs4BTHHNHnUCgf6ngCAjmCu",
      "investigator" : "8Kqd4oWdx4KQGHGGdcHPVdafymUrBvXo6KimREJhttHNhY3JVBKgTCKod1X",
      "linearId" : {
        "externalId" : null,
        "id" : "5ea06290-2dfa-4e0e-8493-a43db61404a0"
      },
      "participants" : [ "8Kqd4oWdx4KQGHGKubAvzAFiUG2JjhHxM2chUs4BTHHNHnUCgf6ngCAjmCu", "8Kqd4oWdx4KQGHGGdcHPVdafymUrBvXo6KimREJhttHNhY3JVBKgTCKod1X" ]
    } ]

We can also see the whistle-blowing case stored on the TradeBody node.

