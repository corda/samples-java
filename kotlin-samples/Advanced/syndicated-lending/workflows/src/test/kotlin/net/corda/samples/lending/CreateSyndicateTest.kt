package net.corda.samples.lending

import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.samples.lending.flows.ApproveLoanBid
import net.corda.samples.lending.flows.CreateSyndicate
import net.corda.samples.lending.flows.SubmitLoanBid
import net.corda.samples.lending.flows.SubmitProjectProposal
import net.corda.samples.lending.states.LoanBidState
import net.corda.samples.lending.states.ProjectState
import net.corda.samples.lending.states.SyndicateState
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.Future

class CreateSyndicateTest {
    private lateinit var network: MockNetwork
    private lateinit var borrower: StartedMockNode
    private lateinit var bankA: StartedMockNode
    private lateinit var bankB: StartedMockNode
    private lateinit var bankC: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(
            MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.samples.lending.flows"),
                TestCordapp.findCordapp("net.corda.samples.lending.contracts")),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))),
                networkParameters = testNetworkParameters(minimumPlatformVersion = 4)
            )
        )
        borrower = network.createNode(MockNodeParameters())
        bankA = network.createNode(MockNodeParameters())
        bankB = network.createNode(MockNodeParameters())
        bankC = network.createNode(MockNodeParameters())
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `Create Syndication flow Test`() {
        //Create the project
        val lenders = listOf(bankA.info.legalIdentities[0],bankB.info.legalIdentities[0])
        val createProjectFlow = SubmitProjectProposal(lenders,"oversea expansion",100,10)
        val future: Future<SignedTransaction> = borrower.startFlow(createProjectFlow)
        network.runNetwork()

        //successful query means the state is stored at node b's vault. Flow went through.
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED)
        val project = bankA.services.vaultService.queryBy(ProjectState::class.java, inputCriteria).states[0].state.data

        //place loan bid
        val projectID = project.linearId
        val submitLoanBidFlow = SubmitLoanBid(borrower.info.legalIdentities[0],7, 5,4.0, 1, projectID)
        val future2: Future<SignedTransaction> = bankA.startFlow(submitLoanBidFlow)
        network.runNetwork()
        val loanBid = borrower.services.vaultService.queryBy(LoanBidState::class.java, inputCriteria).states[0].state.data

        //Borrower approves the loan bid. pick the winning bank
        val loanBidId = loanBid.linearId
        println("---------------------------------")
        println("Submitted Loan State ID : " + loanBidId)
        println("---------------------------------")
        val approveLoanBidFlow = ApproveLoanBid(loanBidId)
        val future3: Future<SignedTransaction> = borrower.startFlow(approveLoanBidFlow)
        network.runNetwork()
        val loanBidApproved = bankA.services.vaultService.queryBy(LoanBidState::class.java, inputCriteria).states[0].state.data
        Assert.assertEquals("APPROVED", loanBidApproved.status)

        //lead bank create Syndication
        val approveLoanBidID = loanBidApproved.linearId
        println("---------------------------------")
        println("Approved Loan State ID : " + approveLoanBidID)
        println("---------------------------------")
        val participatingBanks = listOf(bankB.info.legalIdentities[0],bankC.info.legalIdentities[0])
        val createSyndication = CreateSyndicate(participatingBanks,projectID,approveLoanBidID)
        val future4: Future<SignedTransaction> = bankA.startFlow(createSyndication)
        network.runNetwork()
        val syndication = bankB.services.vaultService.queryBy(SyndicateState::class.java, inputCriteria).states[0].state.data
    }
}