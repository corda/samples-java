# Membership Attestation


In this sample, we will showcase the use of business network extension in a mock insurance constorsium cordapp

Concept:
In this app, we will have a global insurance network, where participants are either insurance companies or different kind of health care providers.
With the help of business network extension, we can further breakdown the global network into smaller pieces as groups, such as APAC_Insurance_Alliance.

In our sample, we will have three nodes, named as:
NetworkProvider
Insurer
CarePro

The NetworkProvider will be create and primarily manage the network. As introduced in the SDK docs, NetworkManager will be the default authorized user of this global network. And the other two nodes will fill the roles which can be easily tell by its name.

The corDapp will run with the following steps:
1. Network creations by NetworkProvider
2. The rest of the nodes request join the network
3. The network provider will query all the request and active the membership status for the other nodes.
4. The NetworkProvider will then create a sub group out of the global insurance network called APAC_Insurance_Alliance, and include the two other nodes in the network.
5. The networkmanager will then assign custom network identity to the nodes. The insurer node will get an insurance identity, the carePro node will get a health care provider identity.
6. Custom network identity comes with custom roles. We will give the insurer node a policy.
   As of now, the network setup is done. The very last step is to run a transaction between the insurer and the carePro node




//Step1: Create the network
flow start CreateNetwork

//Step2: 2 non-member makes the request to join the network.
flow start RequestMembership authorisedParty: NetworkOperator, networkId: 603ec1c1-8b4f-4d4a-968a-8893ba9fdc00

//Step3: go back to the admin node, and query all the membership requests.
flow start QueryAllMembers

//Step4: Admin active membership, two times, ONLY the membership activation
Insurance:
flow start ActiveMembers membershipId: c80bfa2c-d6c4-4376-8244-56d5cd27f050

CarePro:
flow start ActiveMembers membershipId: 54e6d7af-2c9e-4267-adf0-872f70b7c800

---------------------
flow start CreateNetworkSubGroup networkId: 603ec1c1-8b4f-4d4a-968a-8893ba9fdc00, groupName: APAC_Insurance_Alliance, groupParticipants: [baefb1ed-250a-4d27-b3db-6b8914151a45, c80bfa2c-d6c4-4376-8244-56d5cd27f050, 54e6d7af-2c9e-4267-adf0-872f70b7c800]

flow start AssignBNIdentity firmType: InsuranceFirm, membershipId: c80bfa2c-d6c4-4376-8244-56d5cd27f050, bnIdentity: APACIN76CZX

flow start AssignPolicyIssuerRole membershipId: c80bfa2c-d6c4-4376-8244-56d5cd27f050, networkId: 603ec1c1-8b4f-4d4a-968a-8893ba9fdc00

flow start AssignBNIdentity firmType: CareProvider, membershipId: 54e6d7af-2c9e-4267-adf0-872f70b7c800, bnIdentity: APACCP44OJS


//Query to check
run vaultQuery contractStateType: net.corda.core.contracts.ContractState

-------------------Network setup is done, and business flow begins--------------------------

/* Step9: The insurance Company will issue a policy to insuree.
* The flow initiator (the insurance company) has to be a member of the Business network, has to have a insuranceIdentity, and has to have issuer Role, and has to have issuance permission.
  */ 
flow start IssuePolicyInitiator networkId: 603ec1c1-8b4f-4d4a-968a-8893ba9fdc00, careProvider: CarePro, insuree: PeterLi
//Step10: Query the state in the CarePro node.
run vaultQuery contractStateType: net.corda.samples.businessmembership.states.InsuranceState
run vaultQuery contractStateType: net.corda.bn.states.MembershipState


//flow start AssignBNIdentity firmType: CareProvider, membershipId: 9cc6abf7-e20e-4404-be8c-4bcec1a668e0, bnIdentity: APACCP44OJS
