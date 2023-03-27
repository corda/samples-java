# Non-Fungible House Token DvP Sample CorDapp 

This CorDapp provides a basic example to create, issue and perform a DvP (Delivery vs Payment) of an [Evolvable](https://training.corda.net/libraries/token-sdk/token-introduction/#evolvabletokentype), [NonFungible](https://training.corda.net/libraries/token-sdk/token-introduction/#nonfungibletoken) token in 
Corda utilizing the [Token SDK](https://github.com/corda/token-sdk).


## Concepts


### Flows

There are three flows that we'll primarily use in this example that you'll be building off of.

1. We'll start with running `FiatCurrencyIssueFlow`.
2. We'll then create and issue a house token using `HouseTokenCreateAndIssueFlow`.
3. We'll then initiate the sale of the house through `HouseSaleInitiatorFlow`.



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

First go to the shell of PartyA and issue some USD to Party C. We will need the fiat currency to exchange it for the house token. 

    start FiatCurrencyIssueFlow currency: USD, amount: 100000000, recipient: PartyC

We can now go to the shell of PartyC and check the amount of USD issued. Since fiat currency is a fungible token we can query the vault for [FungibleToken](https://training.corda.net/libraries/tokens-sdk/#fungibletoken) states.

    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken
    
Once we have the USD issued to PartyC, we can Create and Issue the HouseToken to PartyB. Goto PartyA's shell to create and issue the house token.
    
    flow start CreateAndIssueHouseToken owner: PartyB, valuationOfHouse: 10000 USD, noOfBedRooms: 2, constructionArea: 1000sqft, additionInfo: NA, address: Mumbai
    
We can now check the issued house token in PartyB's vault. Since we issued it as a [NonFungible](https://training.corda.net/libraries/tokens-sdk/#nonfungibletoken) token we can query the vault for non-fungible tokens.
    
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
    
Note that HouseState token is an evolvable token which is a [LinearState](https://docs.r3.com/en/platform/corda/4.9/community/api-states.html#linearstate), thus we can check PartyB's vault to view the [EvolvableToken](https://training.corda.net/libraries/tokens-sdk/#evolvabletokentype)

    run vaultQuery contractStateType: net.corda.samples.dollartohousetoken.states.HouseState
    
Note the linearId of the HouseState token from the previous step, we will need it to perform our DvP operation. Go to PartyB's shell to initiate the token sale.
    
    flow start HouseSale houseId: <XXXX-XXXX-XXXX-XXXXX>, buyer: PartyC
    
We could now verify that the non-fungible token has been transferred to PartyC and some 100,000 USD from PartyC's vault has been transferred to PartyB. Run the below commands in PartyB and PartyC's shell to verify the same
    
    // Run on PartyB's shell
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.FungibleToken
    // Run on PartyC's shell
    run vaultQuery contractStateType: com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
