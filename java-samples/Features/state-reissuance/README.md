<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# State Reissuance Sample CorDapp

This CorDapp serves as a sample for state reissuance feature of Corda. This feature enables developers to break long 
transaction backchains by reissuing a state with a guaranteed state replacement. This is particularly useful in situations
when a party doesn't want to share state history with other parties for privacy or performance concerns.

This samples demonstrates the feature with the help of a linear state, represented by a land title issued on Corda ledger. 
The land title can be transferred multiple times and when the transaction backchain becomes long, the land title could be 
reissued and the transaction backchain could be pruned.

# Pre-Requisites

For development environment setup, please refer to: [Setup Guide](https://docs.r3.com/en/platform/corda/4.10/community/getting-set-up.html).

# Usage

## Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)

    ./gradlew clean build deployNodes

Then type: (to run the nodes)

    ./build/nodes/runnodes

## Interacting with the CorDapp

PartyA issues a land title to PartyB, Go to PartyA's terminal and run the below command

    start IssueLandTitleFlow owner: PartyB, dimensions: 40X50, area: 1200sqft

Verify the land title has been issued correctly by querying the ledgers of PartyA and PartyB using the below command.
PartyA should be issuer and PartyB should be the owner of the land title.

    run vaultQuery contractStateType: net.corda.samples.statereissuance.states.LandTitleState

Once land title has been issued to PartyB, they could transfer it to PartyC. Go to PartyB's terminal and run the below command

    start TransferLandTitleFlow owner: PartyC, plotIdentifier: <plot-identifier>

You could find the `plot-identifier` from the result of the vaultQuery command used earlier to query the ledgers.
![Screenshot](image/1.jpeg)

Verify the land title has been correctly transferred to PartyC by querying the ledgers of PartyA and PartyC using the below command

    run vaultQuery contractStateType: net.corda.samples.statereissuance.states.LandTitleState

Note that PartyB is no more able to see the land title, since they are no longer a party to the state as they have transferred
the title to PartyC. It is currently only visible to PartyA and PartyC.

Consider that PartyC now wants to reissue the title to get rid of the backchain. They need to request a reissuance to the issuer.
Go to PartyC's terminal and run the below command

    start RequestReissueLandStateFlow issuer: PartyA, plotIdentifier: <plot-identifier>

Now a reissuance request is created on the ledgers of PartyA and PartyC, when can be verified using the below command

    run vaultQuery contractStateType: com.r3.corda.lib.reissuance.states.ReissuanceRequest

The issuer could either accept or reject the reissuance request. Let's consider the case where the issuer accepts the 
reissuance request. To accept the request goto PartyA's (issuer) terminal and run the below command

    start AcceptLandReissuanceFlow issuer: PartyA, stateRef: {index: <output-index>, txhash: <trnx-hash>}

The `<output-index>` and `<trnx-hash>` are of the transaction which created the state to the reissued. They can be found
in the `ReissuanceRequest` queried earlier.
![Screenshot](image/2.jpeg)

On successful completion of the above flow, a duplicate land title would be issued, however it would be currently
locked, and it could not be spent. In order to spend it, the older state which was requested to be reissued must be exited
and that would allow the new reissued state to be unlocked and spend.

To exit the older land title run the below command from PartyC's terminal.

    start ExitLandTitleFlow stateRef: {index: <output-index>, txhash: <trnx-hash>}

Once the previous land title is exited, unlock the reissued land title using the below command from PartyC's terminal

    start UnlockReissuedLandStateFlow reissuedRef: {index: <output-index>, txhash: <tx-hash>}, reissuanceLockRef: {index: <output-index>, txhash: <tx-hash>}, exitTrnxId: <tx-hash>

The `reissuedRef` is the stateRef of the reissued state.
 ![Screenshot](image/3.jpeg)
 
 And the `reissuanceLockRef` is the stateRef of the reissuance lock generated, which can queried using `run vaultQuery contractStateType: com.r3.corda.lib.reissuance.states.ReissuanceLock` 
 ![Screenshot](image/4.jpeg)
 And the `exitTrnxId` is the transaction hash of the transaction used to exit the older state.
 ![Screenshot](image/5.jpeg)
 
Note that the lock uses the encumbrance feature of Corda. You can check out the sample on encumbrance [here](https://github.com/corda/samples-java/tree/master/Features/encumbrance-avatar)

Now with all the information input into the function call, the reissue process is completed and the reissued state can be spent freely.

![Screenshot](image/6.jpeg)

You can see the encumbrance field is restored to `null` again, meaning the state is free to be transacted again. 
![Screenshot](image/7.jpeg)

