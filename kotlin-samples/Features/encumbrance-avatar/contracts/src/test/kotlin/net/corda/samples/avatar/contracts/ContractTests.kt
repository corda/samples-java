package net.corda.samples.avatar.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.samples.avatar.contracts.ExpiryContract.Commands.Pass
import net.corda.samples.avatar.states.Avatar
import net.corda.samples.avatar.states.Expiry
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class ContractTests {
    private val ledgerServices: MockServices = MockServices(listOf("net.corda.samples.avatar"))
    var seller = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))
    var buyer = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))

    @Test
    fun thereMustBeTwoOutputs() {
        ledgerServices.ledger {
            // Should fail bid price is equal to previous highest bid
            transaction {
                //failing transaction
                output(AvatarContract.AVATAR_CONTRACT_ID, Avatar(seller.party, "1"))
                command(listOf(seller.publicKey), AvatarContract.Commands.Create())
                fails()
            }
        }
    }

    @Test
    fun encumbranceIndexMustBeSpecified() {
        //not specifying the encumbrance index fails the contract
        ledgerServices.ledger {
            transaction {
                output(AvatarContract.AVATAR_CONTRACT_ID, Avatar(seller.party, "1"))
                output(
                    ExpiryContract.EXPIRY_CONTRACT_ID,
                    Expiry(Instant.now().plus(2, ChronoUnit.MINUTES), "1", seller.party)
                )
                command(listOf(seller.publicKey), AvatarContract.Commands.Create())
                timeWindow(Instant.now(), Duration.ofMinutes(1))
                failsWith("Avatar needs to be encumbered")
            }
        }
        //specifying the encumbrance index while adding output states passes the contract
        ledgerServices.ledger {
            transaction {
                output(AvatarContract.AVATAR_CONTRACT_ID, 1, Avatar(seller.party, "1"))
                output(
                    ExpiryContract.EXPIRY_CONTRACT_ID,
                    0,
                    Expiry(Instant.now().plus(2, ChronoUnit.MINUTES), "1", seller.party)
                )
                command(listOf(seller.publicKey), AvatarContract.Commands.Create())
                timeWindow(Instant.now(), Duration.ofMinutes(1))
                verifies()
            }
        }
    }

    //Specifying time window is mandatory. This is checked in the encumbrance Expiry state.
    @Test
    fun specifyTimeWindow() {
        ledgerServices.ledger {
            transaction {
                //this fails as time window is not specified
                output(AvatarContract.AVATAR_CONTRACT_ID, 1, Avatar(seller.party, "1"))
                output(
                    ExpiryContract.EXPIRY_CONTRACT_ID,
                    0,
                    Expiry(Instant.now().plus(2, ChronoUnit.MINUTES), "1", seller.party)
                )
                command(listOf(seller.publicKey), AvatarContract.Commands.Create())
                fails()
            }
            transaction {
                output(AvatarContract.AVATAR_CONTRACT_ID, 1, Avatar(seller.party, "1"))
                output(
                    ExpiryContract.EXPIRY_CONTRACT_ID,
                    0,
                    Expiry(Instant.now().plus(2, ChronoUnit.MINUTES), "1", seller.party)
                )
                command(listOf(seller.publicKey), AvatarContract.Commands.Create())
                //this will pass once we specify time window
                timeWindow(Instant.now(), Duration.ofMinutes(1))
                verifies()
            }
        }
    }

    //For selling, the Expiry of avatar must be greater than the time window
    @Test
    fun avatarIsRejectedIfItIsExpired() {
        ledgerServices.ledger {
            transaction {
                output(AvatarContract.AVATAR_CONTRACT_ID, Avatar(seller.party, "1"))
                output(
                    ExpiryContract.EXPIRY_CONTRACT_ID,
                    Expiry(Instant.now().plus(2, ChronoUnit.MINUTES), "1", seller.party)
                )
                command(listOf(seller.publicKey), AvatarContract.Commands.Create())
                timeWindow(Instant.now(), Duration.ofMinutes(3))
                fails()
            }
        }
    }

    //For selling, the Expiry of avatar must be greater than the time window
    @Test
    fun expirationDateShouldBeAfterTheTimeWindow() {
        ledgerServices.ledger {
            transaction {
                output(AvatarContract.AVATAR_CONTRACT_ID, 1, Avatar(seller.party, "1"))
                output(
                    ExpiryContract.EXPIRY_CONTRACT_ID,
                    0,
                    Expiry(Instant.now().plus(3, ChronoUnit.MINUTES), "1", seller.party)
                )
                command(listOf(seller.publicKey), AvatarContract.Commands.Create())
                timeWindow(Instant.now(), Duration.ofMinutes(2))
                verifies()
            }
        }
    }

    //test transaction which has encumbered states as inputs
    @Test
    fun transferAvatar() {
        ledgerServices.ledger {
            unverifiedTransaction {
                output(AvatarContract.AVATAR_CONTRACT_ID, "avatarLabel", 1, Avatar(seller.party, "1"))
                output(
                    ExpiryContract.EXPIRY_CONTRACT_ID,
                    "expiryLabel",
                    0,
                    Expiry(Instant.now().plus(3, ChronoUnit.MINUTES), "1", seller.party)
                )
            }
            transaction {
                input("avatarLabel")
                input("expiryLabel")
                output(AvatarContract.AVATAR_CONTRACT_ID, "avatarLabel2", 1, Avatar(buyer.party, "1"))
                output(
                    ExpiryContract.EXPIRY_CONTRACT_ID,
                    "expiryLabel2",
                    0,
                    Expiry(Instant.now().plus(3, ChronoUnit.MINUTES), "1", buyer.party)
                )
                command(listOf(seller.publicKey, buyer.publicKey), AvatarContract.Commands.Transfer())
                command(listOf(seller.publicKey, buyer.publicKey), Pass())
                timeWindow(Instant.now(), Duration.ofMinutes(2))
                verifies()
            }
        }
    }


    @Test
    fun avatarCannotBeSpentWithoutExpiry() {
        ledgerServices.ledger {
            unverifiedTransaction {
                output(AvatarContract.AVATAR_CONTRACT_ID, "avatarLabel", 1, Avatar(seller.party, "1"))
                output(
                    ExpiryContract.EXPIRY_CONTRACT_ID,
                    "expiryLabel",
                    0,
                    Expiry(Instant.now().plus(3, ChronoUnit.MINUTES), "1", seller.party)
                )
            }
            transaction {
                input("avatarLabel")
                output(AvatarContract.AVATAR_CONTRACT_ID, "avatarLabel2", 1, Avatar(buyer.party, "1"))
                output(
                    ExpiryContract.EXPIRY_CONTRACT_ID,
                    "expiryLabel2",
                    0,
                    Expiry(Instant.now().plus(3, ChronoUnit.MINUTES), "1", buyer.party)
                )
                command(listOf(seller.publicKey, buyer.publicKey), AvatarContract.Commands.Transfer())
                command(listOf(seller.publicKey, buyer.publicKey), Pass())
                timeWindow(Instant.now(), Duration.ofMinutes(2))
                fails()
            }
        }
    }

}