package net.corda.samples.avatar.contracts;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.avatar.states.Avatar;
import net.corda.samples.avatar.states.Expiry;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {

    static private final MockServices ledgerServices = new MockServices();
    static private final TestIdentity seller = new TestIdentity(new CordaX500Name("MegaCorp", "London", "GB"));
    static private final TestIdentity buyer = new TestIdentity(new CordaX500Name("MiniCorp", "London", "GB"));


    //Both the encumbrance and the encumbered state must be added to the transaction
    @Test
    public void thereMustBeTwoOutputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(AvatarContract.AVATAR_CONTRACT_ID, new Avatar(seller.getParty(), "1"));
                tx.command(ImmutableList.of(seller.getPublicKey()), new AvatarContract.Commands.Create());
                tx.fails();
                return null;
            });
            return null;
        }));
    }


    @Test
    public void encumbranceIndexMustBeSpecified() {
        //not specifying the encumbrance index fails the contract
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(AvatarContract.AVATAR_CONTRACT_ID, new Avatar(seller.getParty(), "1"));
                tx.output(ExpiryContract.EXPIRY_CONTRACT_ID,
                        new Expiry(Instant.now().plus(2, ChronoUnit.MINUTES), "1", seller.getParty()));
                tx.command(ImmutableList.of(seller.getPublicKey()), new AvatarContract.Commands.Create());
                tx.timeWindow(Instant.now(), Duration.ofMinutes(1));
                tx.failsWith("Avatar needs to be encumbered");

                return null;
            });
            return null;
        }));

        //specifying the encumbrance index while adding output states passes the contract
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(AvatarContract.AVATAR_CONTRACT_ID, 1, new Avatar(seller.getParty(), "1"));
                tx.output(ExpiryContract.EXPIRY_CONTRACT_ID, 0,
                        new Expiry(Instant.now().plus(2, ChronoUnit.MINUTES), "1", seller.getParty()));
                tx.command(ImmutableList.of(seller.getPublicKey()), new AvatarContract.Commands.Create());
                tx.timeWindow(Instant.now(), Duration.ofMinutes(1));
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    //Specifying time window is mandatory. This is checked in the encumbrance Expiry state.
    @Test
    public void specifyTimeWindow() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                //this fails as time window is not specified
                tx.output(AvatarContract.AVATAR_CONTRACT_ID, 1, new Avatar(seller.getParty(), "1"));
                tx.output(ExpiryContract.EXPIRY_CONTRACT_ID, 0, new Expiry(Instant.now().plus(2, ChronoUnit.MINUTES), "1", seller.getParty()));
                tx.command(ImmutableList.of(seller.getPublicKey()), new AvatarContract.Commands.Create());
                tx.fails();

                //this will pass once we specify time window
                tx.timeWindow(Instant.now(), Duration.ofMinutes(1));
                return tx.verifies();
            });
            return null;
        }));
    }

    //For selling, the Expiry of avatar must be greater than the time window
    @Test
    public void avatarIsRejectedIfItIsExpired() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(AvatarContract.AVATAR_CONTRACT_ID, 1, new Avatar(seller.getParty(), "1"));
                tx.output(ExpiryContract.EXPIRY_CONTRACT_ID, 0, new Expiry(Instant.now().plus(2, ChronoUnit.MINUTES), "1", seller.getParty()));
                tx.command(ImmutableList.of(seller.getPublicKey()), new AvatarContract.Commands.Create());
                tx.timeWindow(Instant.now(), Duration.ofMinutes(3));
                tx.fails();
                return null;
            });
            return null;
        }));
    }

    //For selling, the Expiry of avatar must be greater than the time window
    @Test
    public void expirationDateShouldBeAfterTheTimeWindow() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(AvatarContract.AVATAR_CONTRACT_ID, 1, new Avatar(seller.getParty(), "1"));
                tx.output(ExpiryContract.EXPIRY_CONTRACT_ID, 0, new Expiry(Instant.now().plus(3, ChronoUnit.MINUTES), "1", seller.getParty()));
                tx.command(ImmutableList.of(seller.getPublicKey()), new AvatarContract.Commands.Create());
                tx.timeWindow(Instant.now(), Duration.ofMinutes(2));
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    //test transaction which has encumbered states as inputs
    @Test
    public void transferAvatar() {
        ledger(ledgerServices, ledger -> {
            ledger.unverifiedTransaction(tx -> {
                tx.output(AvatarContract.AVATAR_CONTRACT_ID, "avatarLabel", 1,
                        new Avatar(seller.getParty(), "1"));
                tx.output(ExpiryContract.EXPIRY_CONTRACT_ID, "expiryLabel", 0,
                        new Expiry(Instant.now().plus(3, ChronoUnit.MINUTES), "1", seller.getParty()));
                return null;
            });
            ledger.transaction(tx -> {
                tx.input("avatarLabel");
                tx.input("expiryLabel");
                tx.output(AvatarContract.AVATAR_CONTRACT_ID, "avatarLabel2", 1,
                        new Avatar(buyer.getParty(), "1"));
                tx.output(ExpiryContract.EXPIRY_CONTRACT_ID, "expiryLabel2", 0,
                        new Expiry(Instant.now().plus(3, ChronoUnit.MINUTES), "1", buyer.getParty()));
                tx.command(ImmutableList.of(seller.getPublicKey(), buyer.getPublicKey()), new AvatarContract.Commands.Transfer());
                tx.command(ImmutableList.of(seller.getPublicKey(), buyer.getPublicKey()), new ExpiryContract.Commands.Pass());
                tx.timeWindow(Instant.now(), Duration.ofMinutes(2));
                tx.verifies();
                return null;
            });
            return null;
        });
    }


    @Test
    public void avatarCannotBeSpentWithoutExpiry() {
        ledger(ledgerServices, ledger -> {
            ledger.unverifiedTransaction(tx -> {
                tx.output(AvatarContract.AVATAR_CONTRACT_ID, "avatarLabel", 1,
                        new Avatar(seller.getParty(), "1"));
                tx.output(ExpiryContract.EXPIRY_CONTRACT_ID, "expiryLabel", 0,
                        new Expiry(Instant.now().plus(3, ChronoUnit.MINUTES), "1", seller.getParty()));
                return null;
            });
            ledger.transaction(tx -> {
                tx.input("avatarLabel");
//                tx.input(expiryLabel);
                tx.output(AvatarContract.AVATAR_CONTRACT_ID, "avatarLabel2", 1,
                        new Avatar(buyer.getParty(), "1"));
                tx.output(ExpiryContract.EXPIRY_CONTRACT_ID, "expiryLabel2", 0,
                        new Expiry(Instant.now().plus(3, ChronoUnit.MINUTES), "1", buyer.getParty()));
                tx.command(ImmutableList.of(seller.getPublicKey(), buyer.getPublicKey()), new AvatarContract.Commands.Transfer());
                tx.command(ImmutableList.of(seller.getPublicKey(), buyer.getPublicKey()), new ExpiryContract.Commands.Pass());
                tx.timeWindow(Instant.now(), Duration.ofMinutes(2));
                tx.fails();
                return null;
            });
            return null;
        });
    }
}