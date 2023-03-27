# Fungible House Token Sample CorDapp

This CorDapp serves as a basic example to create, issue, and move [Fungible](https://training.corda.net/libraries/tokens-sdk/#fungibletoken) tokens in Corda utilizing the [Token SDK](https://github.com/corda/token-sdk). In this specific fungible token sample, we will not talk about the redeem method of the Token SDK because the redeem process will take the physical asset off the ledger and destroy the token. Thus, this sample will be a simple walk through of the creation, issuance, and transfer of the tokens.

Quick blog about TokenSDK see [here](https://medium.com/corda/introduction-to-token-sdk-in-corda-9b4dbcf71025)


## Concepts


### Flows

There are a few flows that enable this project. We will create a resource (in this case a house), and then issue tokens for that resource, and then transfer those tokens.


1. We create the representation of a house, within `CreateHouseTokenFlow`.
2. We issue tokens `IssueHouseTokenFlow`
3. We then move the house token. `MoveHouseTokenFlow`


## Pre-Requisites

[Set up for CorDapp development](https://docs.r3.com/en/platform/corda/4.9/community/getting-set-up.html)

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

When started via the command line, each node will display an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.

    Tue July 09 11:58:13 GMT 2019>>>

You can use this shell to interact with your node.

### Fungible Tokens

Create house on the ledger using Seller's interactive node shell, by typing:

    flow start CreateHouseTokenFlow symbol: house, valuationOfHouse: 100000

This will create a linear state of type HouseTokenState in Seller's vault

Seller will now issue some tokens to Buyer. run below command via Seller's interactive node shell:

    flow start IssueHouseTokenFlow symbol: house, quantity: 50, holder: Buyer

Now at Buyer's terminal, we can check the tokens by running:
   
    flow start GetTokenBalance symbol: house

Since Buyer now has 50 tokens, move tokens to Friend from Buyer's terminal

    flow start MoveHouseTokenFlow symbol: house, holder: Friend, quantity: 23

Now lets take look at the balance at both Buyer and their Friend's side 
    
    flow start GetTokenBalance symbol: house
