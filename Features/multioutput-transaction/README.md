<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Multi-Output Transaction Cordapp

In this CorDapp, we will demo how to produce two outputs in one transaction. 
It is a simple use case that we will have two states. One state is called 
SubcountState and the other state is called OmniState, for which the OmniState keeps track of the accumulated balance of 
the SubCountState amounts. Our goal is to show you how to produce two outputs in one transaction.

# Pre-Requisites

For development environment setup, please refer to: [Setup Guide](https://docs.r3.com/en/platform/corda/4.9/community/getting-set-up.html).

# Usage

## Running the nodes

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean build deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```

## Interacting with the nodes
In PartyA's node, run the following commands to initiate a single output transaction to PartyB
```
flow start SendOneOutputFlowInitiator receiver: PartyB, amount: 10
```
Then we can go to PartyB's node to look at the expected output from the first transaction.
```
run vaultQuery contractStateType: net.corda.samples.multioutput.states.SubCountState
```
Now, we would like to bring in the second output. Now, back to PartyA's node, run the following command: 
```
flow start SendTwoOutputFlowInitiator receiver: PartyB, amount: 15
```
we can now go to PartyB's node and look for the second output by: 
```
run vaultQuery contractStateType: net.corda.samples.multioutput.states.OmniCountState
```
Lastly, we would like to show how to introduce new output and update old output at the same transaction. At PartyA's node, run: 
```
flow start UpdateOmniCountFromSubCountFlowInitiator receiver: PartyB, amount: 15
```
And now if we go to PartyB's node and look for the OmniState, we should see it now shows 30.






