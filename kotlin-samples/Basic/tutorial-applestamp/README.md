# Apple Stamp CorDapp

This CorDapp allows a node to create and issue an apple stamp to another node.
Example use of an apple stamp: "One stamp can be exchanged for a basket of Honey Crisp Apples".


## States
* `AppleStamp`: This is a [LinearState](https://docs.r3.com/en/platform/corda/4.9/community/api-states.html#linearstate) that represents an Apple Stamp that can be issued by one party (issuer) to another party (holder).
* `BasketOfApples`: This state represents a specific basket of apples (description of the brand/type, farm, owner, and weight). To change ownership of this basket of apples, the `changeOwner` function can be used.

## Contracts
* `AppleStampContract`: This is used to govern the evolution of an `AppleStamp` state. This file includes validation rules governing the `Issue` command for `AppleStamp`.
* `BasketOfApplesContract`: This is used to govern the evolution of an `BasketOfApples` state. This file includes validation rules governing the `packBacket` and the `Redeem` command for a `BasketOfApples`.

### Flows

* `CreateAndIssueAppleStampInitiator` and `CreateAndIssueAppleStampResponder` flows are used to create and issue an `AppleStamp` state. It takes 2 arguments as the parameters: the `stampDescription` (String) and the `holder` (Party).
* `PackApplesInitiator` flow is used to create a `BasketOfApples` state in the initiating node's vault. It takes 2 arguments as the parameters: the `appleDescription` (String) and the `weight` (Int).
* `RedeemApplesInitiator` and `RedeemApplesResponder` flows are used to redeem a `BasketOfApples` against an `AppleStamp`. It takes 2 arguments as the parameters: the `buyer` (Party) and the `stampId` (UniqueIdentifier).

## Pre-Requisites

For development environment setup, please refer to: [Setup Guide](https://docs.r3.com/en/platform/corda/4.9/community/getting-set-up.html).


## Running the nodes

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```
This should open up 3 new tabs in the terminal window with Corda interactive shells.

One for the Notary, one for Peter, and one for Apple Farm.
(If any of the nodes is missing a Corda interactive shell, from the root folder, navigate to ```./build/node/{missing party node}``` and run ```java -jar corda.jar``` to boot up the Corda interactive shell manually.)

## Interacting with the CorDapp via the terminal

1. Navigate to Apple Farm's Corda Interactive Shell and type the following command:
```
flow start CreateAndIssueAppleStampInitiator stampDescription: "FujiApples", holder: Peter
```
Apple Farm has now created and issued an Apple Stamp that is redeemable against a basket of fuji apples to Peter.

2. Next, in Apple Farm's Corda Interactive Shell, type the following command:
```
flow start PackApplesInitiator appleDescription: "FujiApples", weight: 5
```
Apple Farm has now packed one basket of fuji apples.

3. To check that this `BasketOfApples` state has been successfully stored in Apple Farm's vault, in Apple Farm's Corda Interactive Shell, type the following command:
```
run vaultQuery contractStateType: com.tutorial.states.BasketOfApples
```
The output should be the `BasketOfApples` state that you have just created.

4. If you type the same command into Peter's Corda Interactive Shell, it shouldn't be there as he is not the owner of this state (yet). In order to make him the owner of this basket of fuji apples, he will need to redeem it using his `AppleStamp`. To find out the unique identifier of the `AppleStamp` type the following command into Peter's Corda Interactive Shell:
```
run vaultQuery contractStateType: com.tutorial.states.AppleStamp
```
From the output, copy the value of `id` in `linearId` field.

5. Navigate to Apple Farm's Corda Interactive Shell and type in the following command:
```
flow start RedeemApplesInitiator buyer: Peter, stampId: {paste the copied linearID here}
```
Upon completion of the flow, you should see the output `Flow completed with result: SignedTransaction(id=XXXXXXX)`.
Now Peter has successfully redeemed his `AppleStamp` against a `BasketOfApples`, we can double-check to make sure that this `BasketOfApples` is stored in his vault.

6. In Peter's Corda Interactive Shell, type:
```
run vaultQuery contractStateType: com.tutorial.states.BasketOfApples
```
You should now be able to see the respective `BasketOfApples` in Peter's vault.


