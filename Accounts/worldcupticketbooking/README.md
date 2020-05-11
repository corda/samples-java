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

    start CreateAndShareAccountFlow accountName : dealer1 , partyToShareAccountInfoToList : BCCI

Run the above flow on the Dealer1 node. This will create an account on the Dealer1 node and share this account info with BCCI node.
partyToShareAccountInfoTo will be modified later to take in a list so that account can be shared with multiple nodes.
The above flow will create an account named dealer1 on Dealer1 node. Similarly create below accounts on Dealer1 node.

    start CreateAndShareAccountFlow  accountName : buyer1 , partyToShareAccountInfoToList : Bank
    start CreateAndShareAccountFlow  accountName : buyer2 , partyToShareAccountInfoToList : Bank
    
The above flows will craete accounts named buyer1 and buyer2 on Dealer1's node and will share this account info with the Bank node.

Run the below query to confirm if accounts are created on Dealer1 node. Also run the above query on Bank and BCCI node to confirm if account info is shared with these nodes.

    run vaultQuery contractStateType : com.r3.corda.lib.accounts.contracts.states.AccountInfo



##  Step 2

    start IssueCashFlow accountName : buyer1 , currency : USD , amount : 20

Run the above command on the Bank node, which will issue 20 USD to buyer1 account.

##  Step 3
```
flow start QuerybyAccount whoAmI: buyer1
```
You can check balance of buyer1 account at Dealer1's node
[Option] You can also run the below command to confirm if 20 USD fungible tokens are stored at Dealer1's node. The current holder field in the output will be an AnonymousParty which specifies an account.
```
run vaultQuery contractStateType : com.r3.corda.lib.tokens.contracts.states.FungibleToken
```


##  Step 4

    start CreateT20CricketTicketTokenFlow ticketTeam : MumbaiIndiansVsRajasthanRoyals
    
Run the above flow on BCCI's node. BCCI node will create base token type for the T20 Ticket for the match MumbaiIndians Vs RajasthanRoyals. The ticket ID returned from this flow will be needed in the next steps.
You can see your ticket state generated via vault query at the BCCI'd node:


    run vaultQuery contractStateType : com.t20worldcup.states.T20CricketTicket

##  Step 5

    start IssueNonFungibleTicketFlow tokenId : <XXX-XXX-XXXX-XXXXX>, dealerAccountName : dealer1

Run the above flow on BCCI's node to issue a non fungible token based off the token type which we created in Step5. You will need to replace the `<XXX-XXX-XXXX-XXXXX>` with the uuid returned from step 6. This token will be issued by the BCCI node to dealer1 account on Dealer1 node. 
Switching to the Dealer1's node, you can run the following code to confirm if the token has been issued to the dealer1 account. The current holder it will be a key representing the account.

    run vaultQuery contractStateType : com.r3.corda.lib.tokens.contracts.states.NonFungibleToken


##  Step 6
```
flow start BuyT20CricketTicket tokenId: <XXX-XXX-XXXX-XXXXX>, buyerAccountName: buyer1, sellerAccountName: dealer1, costOfTicker: 5, currency: USD
```

This is the DVP flow where the buyer(buyer1 account on Dealer1 node) account will pay cash to seller account(dealer1 account on Dealer1 node), and the seller accountwill transfer the ticket token to the buyer. Again, replace the `<XXX-XXX-XXXX-XXXXX>` with the uuid generated in step 6.

####  Step 7
```
flow start QuerybyAccount whoAmI: buyer1
flow start QuerybyAccount whoAmI: dealer1
```
Confirm who owns the FungibleToken (cash) and NonFungibleToken (ticket) again by running this on Dealer1's node.


# Further Reading

For accounts visit https://github.com/corda/accounts.

For tokens visit https://github.com/corda/token-sdk.
