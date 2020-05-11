<p align="center">
  <img src="./images/T20-World-Cup-e1578570579412.jpg" alt="Corda" width="2000">
</p>

# T20 Cricket World Cup Ticket Booking using Accounts and Tokens

This Cordapp shows how to integrate accounts and tokens. 

# Background

This sample shows you how to integrate accounts and tokens. This sample talks about a scenario where typically when the Cricket season starts, BCCI (Board of Control for Cricket) starts selling tickets.
As of now there are multiple dealers whom the BCCI issues tickets and further these dealers sell tickets to their client. We are trying to simulate similar functionality maintaining the entore issuance and selling
of the tickets on Corda Platform.

# Steps to Execute

Required Nodes-

1. BCCI node
2. Bank Node
3. Dealer1 Node
4. Dealer2 Node

Accounts-

Accounts will be created by the Dealer nodes for their clients on their nodes and will be shared with the Bank and BCCI nodes.

Looking at the above diagram follow below mentioned steps to run the application.

##  Step 1

    start CreateAndShareAccountFlow accountName : dealer1 , partyToShareAccountInfoTo : BCCI

Run the above flow on the Dealer1 node. This will create an account on the Dealer1 node and share this account info with BCCI node.
partyToShareAccountInfoTo will be modified later to take in a list so that account can be shared with multiple nodes.
The above flow will create an account named dealer1 on Dealer1 node. Similarly create below accounts on Dealer1 node.

    start CreateAndShareAccountFlow  accountName : buyer1 , partyToShareAccountInfoTo : Bank
    start CreateAndShareAccountFlow  accountName : buyer2 , partyToShareAccountInfoTo : Bank
    
The above flows will craete accounts named buyer1 and buyer2 on Dealer1's node and will share this account info with the Bank node.

Run the below query to confirm if accounts are created on Dealer1 node. Also run the above query on Bank and BCCI node to confirm if account info is shared with these nodes.

    run vaultQuery contractStateType : com.r3.corda.lib.accounts.contracts.states.AccountInfo
