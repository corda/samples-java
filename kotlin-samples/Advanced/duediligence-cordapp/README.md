# Due Diligence CorDapp

This CorDapp is an example of how blockchain can work in the capital market industry. Due diligence is commonly the first step of any action in the capital market. In many of the industry, it is also required by the regulators to prevent fraudulent and risky transactions. Each firm will have its own due-diligence process and standards, but at the same time, the cost of executing due diligence is also bared by them individually.

## App Design

<p align="center">
  <img src="./due-d diagram.png" alt="Corda">
</p>
The above picture is a high level mock overview of a shareable due diligence DLT app. BankA will initiate the original Corporate Records auditing with an auditor. Then it will share the auditing report with BankB to save BankB's cost on getting the same report. Vice versa, BankB can work with a different auditor and produce a different report and share with BankA. In the implementation of this sample CorDapp, we will only cover one type of the file auditing. 

Note: another key feature of this app is whitelisting trusted auditors. It is done by utilizing attachment function in Corda. More samples on how to use attachment can be found in the [Features samples folder](../../Features)


## Pre-Requisites
[Set up for CorDapp development](https://docs.r3.com/en/platform/corda/4.9/community/getting-set-up.html)

## Running the nodes

Deploying nodes: `./gradlew clean build deployNodes`

Starting the nodes: `./build/nodes/runnodes`

Uploading whitelisted Auditors: `./gradlew uploadWhitelists`



## Running the CorDapp
Step #1: At BankA, file the original Corporate Records auditing process with Auditor(Trusted Auditor)
```
flow start RequestToValidateCorporateRecordsInitiator validater: Trusted Auditor, numberOfFiles: 10
```

Step #2: Go to the Trusted Auditor Node, validate the auditing request (This step symbolize the auditing process by this third party auditor). Put in the linearId which was returned in Step #1.
```
flow start ValidateCorporateRecordsInitiator linearId: <XXXX-XXX-XXXX-XXXXX-XXXXXX>
```

Step #3: Go to BankA, run a query to confirm that the request has been validated. You should see the variable `qualification=true`.

```
run vaultQuery contractStateType: net.corda.samples.duediligence.states.CorporateRecordsAuditRequest
```
Then, we will instruct BankA to share a copy of the auditing result with BankB: (Again, You would need put in the linearId returned from Step #1). The parameter `trustedAuditorAttachment` is a [jar file](./contracts/src/main/resources/corporateAuditors.jar) which records the trusted auditors. If BankA used an untrusted auditor to acquire the corporate records auditing report. He will be prohibited to share with anyone because it is valueless effort(in this business use case, Of course you can modify the business use cases).
```
flow start ShareAuditingResultInitiator AuditingResultID: <XXXX-XXX-XXXX-XXXXX-XXXXXX>, sendTo: BankB, trustedAuditorAttachment: "8DF3275D80B26B9A45AB022F2FDA4A2ED996449B425F8F2245FA5BCF7D1AC587"
```
This flow will return the linearId of the copy of auditing report, you would need this in Step #6.

Step #4: Go to BankB, run a query to confirm the delivery of copy of the Auditing Report.
```
run vaultQuery contractStateType: net.corda.samples.duediligence.states.CopyOfCoporateRecordsAuditRequest
```
As of now, the sharing of the trusted auditing report is done. What's left now for both BankA and BankB in this use case is to upload the Corporate Records auditing report into a due-diligence list, which they can share with a regulator.(You can again alter this step to suit any other use cases).


Step #5: Go to BankA, attach the Corporate Records auditing report into a due-diligence checklist and report to the Regulator. Again, the approvalId is the linearId returned in Step #1.
```
flow start CreateCheckListAndAddApprovalInitiator reportTo: Regulator, approvalId: <XXXX-XXX-XXXX-XXXXX-XXXXXX>

```
Step #6: Go to BankB, attach the copy of the Corporate Records auditing report into a due-diligence checklist and report to the Regulator. You would need the linearId that is return from Step #3
```
flow start CreateCheckListAndAddApprovalInitiator reportTo: Regulator, approvalId: <XXXX-XXX-XXXX-XXXXX-XXXXXX-Returned-From-Step #3>
```
Step #7: Go to Regulator, run a query on reported due-diligence checklists. You will be able to see both BankA and BankB had filed a due-diligence checklist.
```
run vaultQuery contractStateType: net.corda.samples.duediligence.states.DueDChecklist

```
This use of Distributed Ledger Technology Corda has helped BankB save the costs of going through corporate records due-diligence process whilst still reaching a trusted auditing report. 
