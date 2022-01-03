<p align="center">
    <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Corda encumbrance sample

Corda supports the idea of "Linked States", using the TransactionState.encumbrance property. When building a transaction, a state x can 
point to other state y by specifying the index of y's state in the transaction output index. 
In this situation x is linked to y, i.e. x is dependent on y. x cannot be consumed unless you consume y.
Hence if you want to consume x, y should also be present in the input of this transaction.
Hence y's contract is also always run, when x is about to be consumed. 
In this situation, x is the encumbered state, and y is the encumbrance.
At present, if you do not specify any encumbrance, it defaults to NULL. 

There are many use cases which can use encumbrance like -
1. Cross chain Atomic Swaps
2. Layer 2 games like https://github.com/akichidis/lightning-chess etc.

## About this sample

This is a basic sample which shows how you can use encumbrance in Corda. For this sample, we will have an Avatar
created on Corda. We will transfer this Avatar from one party to the other within a specified time limit.
After this time window, the Avatar will be expired and you cannot transfer it to anyone. 

Avatar state is locked up by the Expiry state which suggests that the Avatar will expire after a certain time, 
and cannot be transferred to anyone after that.

This sample can be extended further, where the Avatar can be represented as a NFT using Corda's Token SDK, and 
can be traded and purchased by a buyer on the exchange. The tokens can be locked up using an encumbrance before 
performing the DVP for the NFT against the tokens.

## How to use run this sample

Build the CorDapp using below command. This will deploy three nodes - buyer, seller and notary.

     ./gradlew clean deployNodes

Execute below commands for all the nodes. This will run the migration scripts on all the nodes.

      cd buil/nodes/PartyA
      java -jar corda.jar run-migration-scripts --core-schemas 
      java -jar corda.jar run-migration-scripts --app-schemas
      java -jar corda.jar 

      cd buil/nodes/PartyB
      java -jar corda.jar run-migration-scripts --core-schemas
      java -jar corda.jar run-migration-scripts --app-schemas
      java -jar corda.jar
      
      cd buil/nodes/Notary
      java -jar corda.jar run-migration-scripts --core-schemas
      java -jar corda.jar run-migration-scripts --app-schemas
      java -jar corda.jar

Create the Avatar on PartyA node

      start CreateAvatarFlow avatarId : 1, expiryAfterMinutes : 3

Sell the Avatar to PartyB node from PartyA node

      start SellAvatarFlow avatarId : 1, buyer : PartyB

Confirm if PartyB owns the Avatar

      run vaultQuery contractStateType : com.template.states.Avatar

Note
As you can see in both the flows, Avatar is encumbered by Expiry. But Encumbrances should form a complete directed cycle, 
otherwise one can spend the "encumbrance" (Expiry) state, which would freeze the "encumbered" (Avatar) state for ever.
That's why we also make Expiry dependent on Avatar. (See how we have added encumbrance index's to the output states in 
both the flows.)

## Reminder

This project is open source under an Apache 2.0 licence. That means you
can submit PRs to fix bugs and add new features if they are not currently
available.