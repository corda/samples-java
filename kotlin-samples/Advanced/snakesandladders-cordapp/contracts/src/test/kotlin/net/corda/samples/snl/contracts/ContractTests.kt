package net.corda.samples.snl.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.samples.snl.states.BoardConfig
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*
import kotlin.collections.LinkedHashMap

class ContractTests {
    private val p1 = TestIdentity(CordaX500Name("PL1", "", "IN"))
    private val p2 = TestIdentity(CordaX500Name("PL2", "", "IN"))
    private val ledgerServices = MockServices(TestIdentity(CordaX500Name("TestId", "", "IN")))

    private val boardConfig_f = BoardConfig(null, null, Arrays.asList(p1.party, p2.party))
    private val boardConfig_s = BoardConfig(LinkedHashMap(Collections.singletonMap(1, 5)),
            LinkedHashMap(Collections.singletonMap(1, 5)), Arrays.asList(p1.party, p2.party))

    @Test
    fun testVerifyCreateBoardZeroInputs() {
        ledgerServices.ledger {
            // Should fail bid price is equal to previous highest bid
            transaction {
                // Has an input, will fail.
                input(BoardConfigContract.ID, boardConfig_s)
                output(BoardConfigContract.ID, boardConfig_s)
                command(Arrays.asList(p1.publicKey, p2.publicKey), BoardConfigContract.Commands.Create())
                fails()
            }
            //pass
            transaction {
                // Has no input, will verify.
                output(BoardConfigContract.ID, boardConfig_s)
                command(Arrays.asList(p1.publicKey, p2.publicKey), BoardConfigContract.Commands.Create())
                verifies()
                verifies()
            }
        }
    }

    @Test
    fun testVerifyCreateBoardNonEmptySnakeAndLadderPositions() {
        ledgerServices.ledger {
            // Should fail bid price is equal to previous highest bid
            transaction {
                // Has empty/ null snake and ladder positions, should fail
                output(BoardConfigContract.ID, boardConfig_f)
                command(Arrays.asList(p1.publicKey, p2.publicKey), BoardConfigContract.Commands.Create())
                fails()
                fails()
            }
            //pass
            transaction {
                // Has no input, will verify.
                output(BoardConfigContract.ID, boardConfig_s)
                command(Arrays.asList(p1.publicKey, p2.publicKey), BoardConfigContract.Commands.Create())
                verifies()
                verifies()
            }
        }
    }
}