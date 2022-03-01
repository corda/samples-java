<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Syndicated Lending Sample Cordapp

This is a sample Cordapp which demonstrate a high level Syndicated Lending scenario on a Corda network. 

Syndicated lending comprises of multiple banks coming together to service the loan requirement of a borrower. 
Generally a lead bank is appointed who coordinated the process to forming a syndicate with other participating banks 
and agreeing on loan terms.

# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```

## Interacting with the CorDapp

PeterCo (as a borrower) starts the `SubmitProjectProposalFlow` in order to submit the project details to a group of 
lenders and request for funds. 

Go to PeterCo's terminal and run the below command

```
start SubmitProjectProposalFlow lenders: [BankOfAshu, BankOfSneha], projectDescription: "Overseas Expansion", projectCost: 10000000, loanAmount: 8000000 
```

Validate the project details are created and shared with the lenders successfully by running the vaultQuery command in each 
lender's terminal and borrower's terminal.

```
run vaultQuery contractStateType: net.corda.samples.lending.states.ProjectState
```

Once the lenders have verified the project details and done their due deligence, they could submit bids for loan.

Goto BankOfAshu's terminal and run the below command. The project-id can be found using the vaultQuery command shown earlier.

```
start SubmitLoanBidFlow borrower: PeterCo, loanAmount: 8000000, tenure: 5, rateofInterest: 4.0, transactionFees: 20000, projectIdentifier: <project_id>
```

Validate the loanBid is submitted successfully by running the vaultQuery command below:

```
run vaultQuery contractStateType: net.corda.samples.lending.states.LoanBidState
```

Now the borrower can inspect the loan terms and approve the loan bid, to start the syndication process.

Go to PeterCo's terminal and run the below command. The loanbid-identifier can be found using the vaultQuery command used earlier.

```
start ApproveLoanBidFlow bidIdentifier: <loanbid-identifier>
```

One the loan bid has been approved by the borrower, the lender can start the process of creating the syndicate by
acting as the lead bank and approach participating bank for funds.

Goto BankOfAshu's terminal and run the below command.

```
start CreateSyndicateFlow participantBanks: [BankOfSneha, BankOfTom], projectIdentifier: <project-identifier>, loanDetailIdentifier: <loanbid-identifier>
```

Verify the syndicate is created using the below command:

```
run vaultQuery contractStateType: net.corda.samples.lending.states.SyndicateState 
```

On receiving the syndicate creation request, participating banks could verify the project and loan terms and submit
bids for the amount of fund they wish to lend by using the below flow in BankOfSneha or BankOfTom node.

```
start SyndicateBidFlow$Initiator syndicateIdentifier: <syndicate-id>, bidAmount: <lending-amount>
```

Verify the syndicate bid is successfully created using the below command:

```
run vaultQuery contractStateType: net.corda.samples.lending.states.SyndicateBidState
```

The lead bank on receiving bids from participating banks could approve the bid using the below flow command.

```
start ApproveSyndicateBidFlow bidIdentifier: <sydicatebid-id>
```