<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Syndicated Lending Sample Cordapp

This is a sample Cordapp which demonstrate a high level Syndicated Lending scenario on a Corda network.

Syndicated lending comprises multiple banks coming together to service the loan requirement of a borrower.
Generally a lead bank is appointed to coordinate the process to forming a syndicate with other participating banks
and agreeing on loan terms.

# Pre-Requisites

[Set up for CorDapp development](https://docs.r3.com/en/platform/corda/4.9/community/getting-set-up.html)

# Usage

## Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean build deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```

## Interacting with the CorDapp

The borrower (PeterCo) starts the `SubmitProjectProposalFlow` in order to submit the project details to a group of
lenders (BankOfAshu, BankOfSneha) and request for funds.

Go to PeterCo's terminal and run the below command

```
start SubmitProjectProposal lenders: [BankOfAshu, BankOfSneha], projectDescription: "Overseas Expansion", projectCost: 10000000, loanAmount: 8000000 
```

To validate that the project details have been created and shared with the lenders successfully by running the vaultQuery command in each
lenders' terminal (BankofAshu, BankOfSneha) and borrower's terminal (PeterCo).

```
run vaultQuery contractStateType: net.corda.samples.lending.states.ProjectState
```

Once the lenders (BankofAshu, BankOfSneha) have verified the project details and done their due diligence, they can submit bids for loan.

Go to BankofAshu's terminal and run the below command. The project-id can be found using the vaultQuery command shown earlier.

```
start SubmitLoanBid borrower: PeterCo, loanAmount: 8000000, tenure: 5, ratioOfInterest: 4.0, transactionFee: 20000, projectIdentifier: <project_id>
```

Validate the loanBid is submitted successfully by running the vaultQuery command below:

```
run vaultQuery contractStateType: net.corda.samples.lending.states.LoanBidState
```

Now the borrower (PeterCo) can inspect the loan terms and approve the loan bid, to start the syndication process.

Go to PeterCo's terminal and run the below command. The loanbid-identifier can be found using the vaultQuery command used earlier.

```
start ApproveLoanBid bidIdentifier: <loanbid-identifier>
```

One the loan bid has been approved by the borrower (PeterCo), the lender (BankOfAshu) can start the process of creating the syndicate by
acting as the lead bank and approach participating bank for funds.

Goto PartyB's terminal and run the below command.

```
start CreateSyndicate participantBanks: [BankOfSneha, BankOfTom], projectIdentifier: <project-identifier>, loanDetailIdentifier: <loanbid-identifier>
```

Verify the syndicate is created using the below command:

```
run vaultQuery contractStateType: net.corda.samples.lending.states.SyndicateState 
```

On receiving the syndicate creation request, participating banks (BankOfSneha, BankOfTom) can verify the project and loan terms and submit
bids for the amount of fund they wish to lend by using the below flow.

```
start SubmitSyndicateBid syndicateIdentifier: <syndicate-id>, bidAmount: <lending-amount>
```

Verify the syndicate bid is successfully created using the below command:

```
run vaultQuery contractStateType: net.corda.samples.lending.states.SyndicateBidState
```

The lead bank (BankOfAshu) on receiving bids from participating banks (BankOfSneha, BankOfTom) could approve the bid using the below flow command.

```
start ApproveSyndicateBid bidIdentifier: <sydicatebid-id>
```