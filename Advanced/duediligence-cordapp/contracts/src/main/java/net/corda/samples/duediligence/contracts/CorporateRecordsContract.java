package net.corda.samples.duediligence.contracts;

import kotlin.text.Charsets;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.duediligence.states.CopyOfCoporateRecordsAuditRequest;
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.samples.duediligence.Constants.CORPORATE_JAR_HASH;

public class CorporateRecordsContract implements Contract {

    public static final String CorporateRecordsContract_ID = "net.corda.samples.duediligence.contracts.CorporateRecordsContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties command = tx.getCommands().get(0);

        //Propose request
        if (command.getValue() instanceof Commands.Propose) {
            requireThat(require -> {
                require.using("There are no inputs", tx.getInputs().isEmpty());
                require.using("Only one output state should be created.", tx.getOutputs().size() == 1);
                require.using("The single output is of type CorporateRecords State", tx.outputsOfType(CorporateRecordsAuditRequest.class).size() == 1);
                return null;
            });
        }else if (command.getValue() instanceof Commands.Validate) { //Validate Rules
            requireThat(require -> {
                require.using("Only one output state should be created.", tx.getOutputs().size() == 1);
                require.using("The single output is of type CorporateRecords State", tx.outputsOfType(CorporateRecordsAuditRequest.class).size() == 1);
                return null;
            });
        }else if (command.getValue() instanceof Commands.Reject) { //Rejection Rules
            requireThat(require -> {
                require.using("Only one output state should be created.", tx.getOutputs().size() == 1);
                require.using("The single output is of type CorporateRecords State", tx.outputsOfType(CorporateRecordsAuditRequest.class).size() == 1);
                return null;
            });
        }
        else if (command.getValue() instanceof Commands.Share) { //Share Rules

            // Check if the report has any value to share among other network participants.
            List<Attachment> nonContractAttachments = tx.getAttachments()
                    .stream()
                    .filter(p -> !(p instanceof ContractAttachment))
                    .map(p -> (Attachment) p)
                    .collect(Collectors.toList());

            Attachment attached = nonContractAttachments.get(0);

            ContractsDSL.requireThat(req -> {
                req.using("The transaction should have a single non-contract attachment",
                        nonContractAttachments.size() == 1);
                req.using("The jar's hash should be correct", attached.getId().equals(CORPORATE_JAR_HASH));
                return null;
            });

            // Extract the whitelisted company names from the JAR.
            List<String> whitelistedCompanies = new ArrayList<>();
            JarInputStream attachmentJar = attached.openAsJAR();
            try {
                while (!attachmentJar.getNextEntry().getName().equals("whitelistedCorporateAuditors.txt")) {
                    // Calling 'getNextEntry()' causes us to scroll through the JAR.
                }
                InputStreamReader isrWhitelistlist = new InputStreamReader(attachmentJar, Charsets.UTF_8);
                BufferedReader brWhitelist = new BufferedReader(isrWhitelistlist, (8 * 1024)); // Note - changed BIR to BR

                String company = brWhitelist.readLine();

                while (company != null) {
                    whitelistedCompanies.add(company);
                    company = brWhitelist.readLine();
                }
            } catch (IOException e) {
                System.out.println("error reading whitelistedCorporateAuditors.txt");
            }

            // Constraints on the Whitelist parties
            //CorporateRecordsAuditRequest corporateRecords = tx.outputsOfType(CorporateRecordsAuditRequest.class).get(0);
            //CorporateRecordsAuditRequest corporateRecords = tx.inputsOfType(CorporateRecordsAuditRequest.class).get(0);
            StateAndRef<CorporateRecordsAuditRequest> recordsStateAndRef = tx.referenceInputRefsOfType(CorporateRecordsAuditRequest.class).get(0);
            CorporateRecordsAuditRequest corporateRecords = recordsStateAndRef.getState().getData();

            List<Party> participants = new ArrayList<>();
            List<String> participantsOrgs = new ArrayList<>();
            for (AbstractParty p : corporateRecords.getParticipants()) {
                Party participant = (Party) p;
                participantsOrgs.add(participant.getName().getOrganisation());
                participants.add(participant);
            }

            // overlap is whether any participants in the transaction belong to a whitelisted org.
            Set<String> overlap = new HashSet<>(whitelistedCompanies); //Crossland Savings & TCF National Bank Wisconsin
            overlap.retainAll(new HashSet<>(participantsOrgs)); // intersection | TCF & PartyA

            ContractsDSL.requireThat(req -> {
                req.using("The agreement did not use any whitelisted auditors" + overlap.toString(), (!overlap.isEmpty()));
                return null;
            });
        }
    }

    public interface Commands extends CommandData {
        class Propose implements Commands { }
        class Validate implements Commands { }
        class Reject implements Commands { }
        class Share implements Commands { }
        class Report implements  Commands { }
    }

}
