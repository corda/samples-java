package net.corda.samples.carinsurance.flows

import com.google.common.collect.ImmutableList
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.samples.carinsurance.states.InsuranceState
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = ImmutableList.of(
            TestCordapp.findCordapp("net.corda.samples.carinsurance.contracts"),
            TestCordapp.findCordapp("net.corda.samples.carinsurance.flows")
    ),
            notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
    ))
    private val a = network.createNode()
    private val b = network.createNode()

    @Before
    fun setup() {
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    //simple example test to test if the issue insurance flow only carries one output.
    @Test
    @Throws(Exception::class)
    fun issueInsuranceFlowTest() {

        val car = VehicleInfo(
                "I4U64FY56I48Y",
                "165421658465465",
                "BMW",
                "M3",
                "MPower",
                "Black",
                "gas")

        val policy1 = InsuranceInfo(
                "8742",
                2000,
                18,
                49,
                car)

        val flow: IssueInsurance = IssueInsurance(policy1, b.info.legalIdentities[0])
        val future = a.startFlow<SignedTransaction>(flow)
        network.runNetwork()
        val ptx = future.get()

        //assertion for single output
        Assert.assertEquals(1, ptx.tx.outputStates.size.toLong())
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun issueInsuranceClaimFlowTest() {
        val car = VehicleInfo(
                "I4U64FY56I48Y",
                "165421658465465",
                "BMW",
                "M3",
                "MPower",
                "Black",
                "gas")
        val policy1 = InsuranceInfo(
                "8742",
                2000,
                18,
                49,
                car)
        val flow = IssueInsurance(policy1, b.info.legalIdentities[0])
        val future = a.startFlow(flow)
        network.runNetwork()

        val claimInfo = ClaimInfo("C001", "Minor Accident, Bumper Damaged", 1000)
        val policyNumber = "8742"
        val flow2 = InsuranceClaim(claimInfo, policyNumber)
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        val ptx2 = future2.get()

        //assertion for single output
        val inState = ptx2.tx.getOutput(0) as InsuranceState
        Assert.assertNotNull(inState)
    }
}



