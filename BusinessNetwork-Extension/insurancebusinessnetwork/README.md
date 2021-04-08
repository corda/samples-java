# Insurance Business Network


In this sample, we will showcase the use of business network extension in a mock insurance constorsium cordapp

<p align="center">
  <img src="./MockDiagram.jpeg" alt="Corda">
</p>

### Concept:
In this app, we will have a global insurance network, where participants are either insurance companies or different kind of health care providers.
With the help of business network extension, we can further breakdown the global network into smaller pieces as groups, such as APAC_Insurance_Alliance.

In our sample, we will have three nodes, named as:
* NetworkOperator <- Business Network Operator
* Insurance <- Insurance Company that is in the network
* CarePro <- Care Provider of the network

The NetworkOperator will be create and primarily manage the network. As introduced in the SDK docs, NetworkOperator will be the default authorized user of this global network. And the other two nodes will fill the roles which can be easily tell by its name.

#### The corDapp will run with the following steps:
1. Network creations by NetworkOperator
2. The rest of the nodes request join the network
3. The NetworkOperator will query all the request and active the membership status for the other nodes.
4. The NetworkOperator will then create a sub group out of the global insurance network called APAC_Insurance_Alliance, and include the two other nodes in the network.
5. The NetworkOperator will then assign custom network identity to the nodes. The insurer node will get an insurance identity, the carePro node will get a health care provider identity.
6. Custom network identity comes with custom roles. We will give the insurer node a policy.
   As of now, the network setup is done. The very last step is to run a transaction between the insurer and the carePro node

### Usage

#### Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```
#### Interacting with the CorDapp

Step1: Create the network in NetwprkOperator's terminal
```
flow start CreateNetwork
```

Step2: 2 non-member makes the request to join the network. Fill in the networkId with what was return from Step1
```
flow start RequestMembership authorisedParty: NetworkOperator, networkId: <xxxx-xxxx-xxxx-xxxx-xxxxx> 
```
Step3: go back to the admin node, and query all the membership requests.
```
flow start QueryAllMembers
```
Step4: Admin active membership, two times, ONLY the membership activation
Insurance: fill in the Insurance node MembershipId that is display in the previous query
```
flow start ActiveMembers membershipId: <xxxx-xxxx-xxxx-xxxx-xxxxx>
```
CarePro: fill in the CarePro node MembershipId that is display in the previous query
```
flow start ActiveMembers membershipId: <xxxx-xxxx-xxxx-xxxx-xxxxx>
```

Step5: Admin create subgroup and add group members. 
```
flow start CreateNetworkSubGroup networkId: <xxxx-FROM-STEP-ONE-xxxxx>, groupName: APAC_Insurance_Alliance, groupParticipants: [<xxxx-NETWORKOPERATOR-ID-xxxxx>, <xxxx-xxxx-INSURANCE-ID-xxxxx>, <xxxx-xxxx-CAREPRO-ID-xxxxx>]
```
Step6: Admin assign business identity to a member. 
```
flow start AssignBNIdentity firmType: InsuranceFirm, membershipId: <xxxx-xxxx-INSURANCE-ID-xxxxx>, bnIdentity: APACIN76CZX
```
Step7: Admin assign business identity to the second member 
```
flow start AssignBNIdentity firmType: CareProvider, membershipId: <xxxx-xxxx-CAREPRO-ID-xxxxx>, bnIdentity: APACCP44OJS
```
Step8: Admin assign business identity related ROLE to the member.
```
flow start AssignPolicyIssuerRole membershipId: <xxxx-xxxx-INSURANCE-ID-xxxxx>, networkId: <xxxx-xxxx-NETWORK-ID-xxxxx>
```
Sanity Check: Query to check, we should be able to see multiple MembershipStates and GroupStates
```
run vaultQuery contractStateType: net.corda.core.contracts.ContractState
run vaultQuery contractStateType: net.corda.bn.states.MembershipState
```
-------------------Network setup is done, and business flow begins--------------------------

Step9: The insurance Company will issue a policy to insuree. The flow initiator (the insurance company) has to be a member of the Business network, has to have a insuranceIdentity, and has to have issuer Role, and has to have issuance permission.
```
flow start IssuePolicyInitiator networkId: 603ec1c1-8b4f-4d4a-968a-8893ba9fdc00, careProvider: CarePro, insuree: PeterLi
```
Step10: Query the state in the CarePro node.
```
run vaultQuery contractStateType: net.corda.samples.businessmembership.states.InsuranceState
```
