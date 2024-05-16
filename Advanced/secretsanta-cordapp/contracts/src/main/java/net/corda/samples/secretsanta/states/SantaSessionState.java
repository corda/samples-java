package net.corda.samples.secretsanta.states;

import net.corda.samples.secretsanta.contracts.SantaSessionContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// *********
// * State *
// *********
@BelongsToContract(SantaSessionContract.class)
public class SantaSessionState implements ContractState, LinearState {

    // private variables
    private UniqueIdentifier linearId;

    private List<String> playerNames;
    private List<String> playerEmails;
    private LinkedHashMap<String, String> assignments;

    private Party issuer; // issuer is the creator of the santa session
    private Party owner; // owner is the receiver of the santa session


    // Note Corda always needs a constructor for deserialization in java!
    @ConstructorForDeserialization
    public SantaSessionState(UniqueIdentifier linearId, List<String> playerNames, List<String> playerEmails, LinkedHashMap<String, String> assignments, Party issuer, Party owner) {
        this.linearId = linearId;
        this.playerNames = playerNames;
        this.playerEmails = playerEmails;
        this.assignments = assignments;
        this.issuer = issuer;
        this.owner = owner;
    }

    public SantaSessionState(List<String> playerNames, List<String> playerEmails, Party issuer, Party owner) {
        this.playerNames = playerNames;
        this.playerEmails = playerEmails;
        this.issuer = issuer;
        this.owner = owner;
        this.linearId = new UniqueIdentifier();

        if (playerNames.size() != playerEmails.size()) {
            throw new IllegalArgumentException("Inconsistent number of names and emails");
        }

        if (playerNames.size() < 3) {
            throw new IllegalArgumentException("Too few players: " + playerNames.size() + " " + playerNames.toString());
        }

        LinkedHashMap<String, String> _assignments = new LinkedHashMap<>();

        ArrayList<String> shuffledPersons = new ArrayList<>(playerNames);
        Collections.shuffle(shuffledPersons);

        for (int i=0; i < playerNames.size() - 1; i++) {
            _assignments.put(shuffledPersons.get(i), shuffledPersons.get(i+1));
        }

        _assignments.put(shuffledPersons.get(shuffledPersons.size()-1), shuffledPersons.get(0));

        this.setAssignments(_assignments);
    }

    // confirm quality of assignments
    public void setAssignments(LinkedHashMap<String, String> assignments) {

        if (assignments == null) {
            throw new IllegalArgumentException("Given null assignments");
        }

        if (playerNames.size() != assignments.size()) {
            throw new IllegalArgumentException("Invalid number of pairings");
        }

        // iterate through assignments for validity
        for (String santa_candidate: playerNames) {

            // ensure all these players exist
            if(!playerNames.contains(santa_candidate)) {
                throw new IllegalArgumentException("A santa candidate does not exist in the list of players");
            }

            for (String target_candidate: playerNames) {

                // skip duplicates in iteration
                if (santa_candidate.equals(target_candidate)) { continue; }

                // ensure no one is assigned themselves
                if(santa_candidate.equals(assignments.get(santa_candidate))){
                    throw new IllegalArgumentException("player assigned themselves");
                }
            }
        }

        // if assignments look good, we'll set them in the member variable
        this.assignments = assignments;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public void setLinearId(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    public void setPlayerNames(List<String> playerNames) {
        this.playerNames = playerNames;
    }

    public List<String> getPlayerEmails() {
        return playerEmails;
    }

    public void setPlayerEmails(List<String> playerEmails) {
        this.playerEmails = playerEmails;
    }

    public LinkedHashMap<String, String> getAssignments() {
        return assignments;
    }

    public Party getIssuer() {
        return issuer;
    }

    public void setIssuer(Party issuer) {
        this.issuer = issuer;
    }

    public Party getOwner() {
        return owner;
    }

    public void setOwner(Party owner) {
        this.owner = owner;
    }

    /* *
     * This method will indicate who are the participants and required signers when
     * this state is used in a transaction.
     * */
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(issuer, owner);
    }


    // convenience method to get a random element from a set
    public <E> Object chooseRandomElement (Set<String> s) {
        int item = new Random().nextInt(s.size());
        int i = 0;

        for (Object obj : s) {
            if (i == item)
                return obj;
            i++;
        }
        return null;
    }

    /* *
     * get key by value from HashMap
     * from StackOverflow: https://stackoverflow.com/questions/8112975/get-key-from-a-hashmap-using-the-value
     */
    public Object getKeyByFirstValue(LinkedHashMap<String, String> map, Object v) {

        for (Map.Entry<String, String> e: map.entrySet()) {
            Object key = e.getKey();
            Object value = e.getValue();

            if (v.equals(value)){
                return key;
            }
        }

        return null;
    }

}
