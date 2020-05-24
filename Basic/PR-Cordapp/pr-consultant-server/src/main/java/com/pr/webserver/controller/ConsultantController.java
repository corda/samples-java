package com.pr.webserver.controller;


import com.pr.common.data.PRFlowData;
import com.pr.common.data.RequestFlowData;
import com.pr.consultant.initiator.ConsultantInitiator;
import com.pr.consultant.initiator.RequestFlowInitiator;
import com.pr.common.exception.PRException;
import com.pr.contract.state.schema.contracts.PRContract;
import com.pr.contract.state.schema.states.PRState;
import com.pr.contract.state.schema.states.PRStatus;
import com.pr.server.common.ServerConstant;
import com.pr.server.common.bo.impl.PRBO;
import com.pr.server.common.bo.impl.RequestFormBO;
import com.pr.server.common.controller.CommonController;
import com.pr.server.common.exception.PRServerException;
import com.pr.server.common.helper.PRControllerHelper;
import com.pr.student.contract.state.schema.contract.RequestFormContract;
import com.pr.student.contract.state.schema.state.RequestForm;
import com.pr.student.contract.state.schema.state.RequestStatus;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.flows.AbstractCashFlow;
import net.corda.finance.flows.CashIssueFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */


@RestController
@RequestMapping("/consultant") // The paths for HTTP requests are relative to this base path.
@CrossOrigin
public class ConsultantController extends CommonController {

    private final static Logger logger = LoggerFactory.getLogger(ConsultantController.class);


    @CrossOrigin
    @GetMapping(value = "/hello", produces = "text/plain")
    private String uniName() {
        return "Hello Consultant";
    }

    /**
     * @param prData is a json object which we provide as an input to our post api
     * @return It returns status whether PR request is created or not
     */

    @CrossOrigin
    @PostMapping("/")
    private ResponseEntity sendPRRequest(@RequestBody PRBO prData) {
        StateAndRef<PRState> previousPRRequest = null;
        AbstractParty consultantParty = null;

        PRContract.Commands command = new PRContract.Commands.CREATE();

        if (prData == null)
            throw new PRException("Invalid Request!");

        AbstractParty wesParty = getPartyFromFullName("O=Wes,L=London,C=GB");

        Set<Party> partyFrom = connector.getRPCops().partiesFromName(prData.getConsultantParty(), false);
        Iterator<Party> parties = partyFrom.iterator();

        while (parties.hasNext()) {
            consultantParty = parties.next();
        }

        // Creating state
        PRState prState = convertToPRState(prData, PRStatus.APPLICATION_SUBMITTED, consultantParty, wesParty);
        logger.info("PR State:" + prState.toString());

        try {
            FlowHandle<SignedTransaction> flowHandle = connector.getRPCops().startFlowDynamic
                    (ConsultantInitiator.class, new PRFlowData(prState, previousPRRequest, command));
            SignedTransaction signedTransaction = flowHandle.getReturnValue().get();
            logger.info(String.format("signed Tx id: %s", signedTransaction.getId().toString()));
            return ResponseEntity.ok("PR Request created with Txn Id: " + signedTransaction.getId().toString());
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e.getMessage(), e.getCause());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    /**
     * @param amount   is parameter which needs to be transferred to another party
     * @param currency relates to the type of currency (USD, INR)
     * @return It returns status whether money has transferred or not
     */

    @CrossOrigin
    @PutMapping("/cash")
    public ResponseEntity issueCash(@RequestParam("amount") Long amount, @RequestParam("currency") String currency) {
        // 1. Prepare issue request.
        final Amount<Currency> issueAmount = new Amount<>(amount * 100, Currency.getInstance(currency));
        final List<Party> notaries = connector.getRPCops().notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("COULD_NOT_FIND_A_NOTARY");
        }
        final Party notary = notaries.get(0);
        final OpaqueBytes issueRef = OpaqueBytes.of("0".getBytes());
        final CashIssueFlow.IssueRequest issueRequest = new CashIssueFlow.IssueRequest(issueAmount, issueRef, notary);
        // 2. Start flow and wait for response.
        try {
            final FlowHandle<AbstractCashFlow.Result> flowHandle = connector.getRPCops().startFlowDynamic(CashIssueFlow.class, issueRequest);
            final AbstractCashFlow.Result result = flowHandle.getReturnValue().get();
            final String msg = result.getStx().getTx().getOutputStates().get(0).toString();
            return ResponseEntity.status(HttpStatus.CREATED).body(msg);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }



    /**
     * @param id is a  UUID wesReferenceNumber which should be provided while querying state according to wesReferenceNumber
     * @return It returns the state by querying the vault
     * @throws Exception
     */

    @CrossOrigin
    @GetMapping("/")
    public ResponseEntity getPRRequestDetails(@RequestParam(value = "id", required = false) String id) throws Exception {

        if (!StringUtils.isEmpty(id)) {
            try {
                UniqueIdentifier uniqueIdentifier = UniqueIdentifier.Companion.fromString(id);
                Set<Class<PRState>> contractStateTypes = new HashSet(Collections.singletonList(PRState.class));

                QueryCriteria linearCriteria = new QueryCriteria.LinearStateQueryCriteria(null, Arrays.asList(uniqueIdentifier),
                        Vault.StateStatus.UNCONSUMED, contractStateTypes);

                Vault.Page<PRState> results = connector.getRPCops().vaultQueryByCriteria(linearCriteria, PRState.class);


                if (results.getStates().size() > 0) {
                    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsString(results.getStates()));
                } else {
                    return ResponseEntity.status(HttpStatus.OK).body("No Records found");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                List<StateAndRef<PRState>> states = connector.getRPCops().vaultQuery(PRState.class).getStates();
                if (logger.isDebugEnabled()) {
                    states.forEach(e -> logger.debug(e.getState().getData().toString()));
                }
                if (!states.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body
                            (mapper.writeValueAsString(states));
                } else {
                    return ResponseEntity.noContent().build();
                }

        } catch(Exception e){
            e.printStackTrace();
        }
    }

        return new ResponseEntity<>("",HttpStatus.BAD_REQUEST);
}

    /**
     *
     * @param command is a parameter which creates a new Academic form request (CREATE)
     * @param requestFormBO is a json object which we provide as an input to our post api
     * @return It returns status whether new Academic form request has created or not
     */

    @CrossOrigin
    @PostMapping("transcript/{command}")
    public ResponseEntity raiseBookingRequest(@PathVariable("command") String command,
                                              @RequestBody RequestFormBO requestFormBO) {
        RequestFormContract.Commands contractCommand;
        SignedTransaction signedTransaction = null;
        AbstractParty consultantParty = null;
        AbstractParty wesParty = null;
        AbstractParty universityParty = null;
        if (command.equalsIgnoreCase(ServerConstant.CREATE_COMMAND)) {
            contractCommand = new RequestFormContract.Commands.CREATE();
            logger.info("Contract Command" + contractCommand);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unknown Command! " + command);
        }
        RequestForm requestFormState;
        StateAndRef<RequestForm> previousrequestFormState = null;
        if (requestFormBO == null) {
            throw new IllegalArgumentException("Invalid Request!");
        }
        consultantParty = getParty(requestFormBO.getConsultantParty());
        wesParty = getParty(requestFormBO.getWesParty());
        universityParty = getParty(requestFormBO.getUniversityParty());
        requestFormState = convertToRequestFormState(requestFormBO,consultantParty,wesParty,universityParty,RequestStatus.APPLICATION_SUBMITTED);

        try {
            FlowHandle<SignedTransaction> flowHandle = connector.getRPCops().startFlowDynamic
                    (RequestFlowInitiator.class, new RequestFlowData(requestFormState,
                            previousrequestFormState, contractCommand));
            signedTransaction = flowHandle.getReturnValue().get();
            logger.info(String.format("signed Tx id: %s", signedTransaction.getId().toString()));
            return ResponseEntity.ok("Student Transcript Request created with Txn Id: " + signedTransaction.getId().toString());
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e.getMessage(), e.getCause());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /***
     *
     * @param requestId is a UUID which helps to query the state from vault
     * @param requestStatus is a parameter to change request status (APPLICATION_SUBMITTED etc.)
     * @return It returns status whether Transcript details are confirmed or not
     */

    @CrossOrigin
    @PutMapping("transcript/{requestId}")
    public ResponseEntity confirmTranscriptDetails(@PathVariable("requestId") String requestId,
                                                   @RequestParam(value = "requestStatus", required = false, defaultValue = "CONFIRMED") String requestStatus) {
        RequestFormContract.Commands contractCommand;
        SignedTransaction signedTransaction = null;

        if (StringUtils.isEmpty(requestId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("RequestId! is missing. Please enter a valid booking Id.");
        }

        contractCommand = new RequestFormContract.Commands.UPDATE();

        List<StateAndRef<RequestForm>> previousRequest = PRControllerHelper.getRequestFormStateFromLinearId(requestId,connector.getRPCops());

        if (previousRequest == null || previousRequest.isEmpty()) {
            throw new PRServerException("Request Id with id: " + requestId + " doesn't exist please verify and try again!");
        }

        if(!previousRequest.get(0).getState().getData().getRequestStatus().equals(RequestStatus.ADDED_TRANSCRIPT_DETAILS)) {
            throw new IllegalArgumentException("Invalid request status, Expected Status : " + RequestStatus.ADDED_TRANSCRIPT_DETAILS);
        }

        RequestForm newRequestFormState = new RequestForm(previousRequest.get(0).getState().getData(),RequestStatus.CONFIRMED);

        try {
            FlowHandle<SignedTransaction> signedTransactionFlowHandle = connector.getRPCops().startFlowDynamic(RequestFlowInitiator.class,
                    new RequestFlowData(newRequestFormState, previousRequest.get(0),contractCommand));

            signedTransaction = signedTransactionFlowHandle.getReturnValue().get();
            return ResponseEntity.ok("Transcript Details confirmed successfully with Txn ID: " + signedTransaction.getId().toString());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

    }


    

    /**
     *
     * @param abstractParty is a abstract party name as String
     * @return It returns matching abstract party
     */

    private AbstractParty getParty(String abstractParty) {
        AbstractParty party = null;
        Set<Party> partyFrom = connector.getRPCops().partiesFromName(abstractParty.toString(), false);
        Iterator<Party> parties = partyFrom.iterator();

        while (parties.hasNext()) {
            party = parties.next();
        }
        return party;
    }

}