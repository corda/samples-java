package com.pr.webserver.controller;

import com.pr.common.data.PRFlowData;
import com.pr.common.exception.PRException;
import com.pr.contract.state.schema.contracts.PRContract;
import com.pr.contract.state.schema.states.ECAState;
import com.pr.contract.state.schema.states.PRState;
import com.pr.contract.state.schema.states.PRStatus;
import com.pr.server.common.bo.impl.ECAStateBO;
import com.pr.server.common.bo.impl.PRBO;
import com.pr.server.common.controller.CommonController;
import com.pr.server.common.exception.PRServerException;
import com.pr.server.common.helper.PRControllerHelper;
import com.pr.student.contract.state.schema.state.RequestForm;
import com.pr.student.contract.state.schema.state.RequestStatus;
import com.pr.wes.initiator.WesInitiator;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

@RestController
@RequestMapping("/wes") // The paths for HTTP requests are relative to this base path.
@CrossOrigin
public class WesController extends CommonController {

    private final static Logger logger = LoggerFactory.getLogger(WesController.class);


    @CrossOrigin
    @GetMapping(value = "/hello", produces = "text/plain")
    private String uniName() {
        return "Hello Wes";
    }

    /**
     * @param requestId is UUID which helps to update the state
     * @param prbo      is a json object which we provide as an input to our post api
     * @return It returns status whether PR request is updated or not
     * @throws Exception
     */


    @CrossOrigin
    @PutMapping("/{requestId}")
    public ResponseEntity respondToPrRequest(@PathVariable(value = "requestId", required = true) String requestId, @RequestBody PRBO prbo) throws Exception {

        if (StringUtils.isEmpty(requestId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid requestId!");
        }
        PRState newPRState = null;
        PRContract.Commands command = new PRContract.Commands.RequestApproval();
        PRStatus prStatus = null;

        List<StateAndRef<PRState>> previousPrState = PRControllerHelper.getPrStateFromRequestId(requestId, connector.getRPCops());

        List<StateAndRef<RequestForm>> previousRequestFormState = PRControllerHelper.getRequestFormStateFromRequestId(requestId, connector.getRPCops());

        if (previousPrState == null || previousPrState.isEmpty()) {
            throw new PRException("PR Request with id: " + requestId + " doesn't exist please verify and try again!");
        }

        if (previousPrState.get(0).getState().getData().getPrStatus().equals(PRStatus.APPLICATION_SUBMITTED))
            prStatus = PRStatus.APPLICATION_ACKNOWLEDGEMENT;
        else if (previousPrState.get(0).getState().getData().getPrStatus().equals(PRStatus.APPLICATION_ACKNOWLEDGEMENT)
                && previousRequestFormState.get(0).getState().getData().getRequestStatus().equals(RequestStatus.APPLICATION_READY_FOR_WES_VERIFICATION))
            prStatus = PRStatus.DOCUMENT_RECEIVED;
        else if (previousPrState.get(0).getState().getData().getPrStatus().equals(PRStatus.DOCUMENT_RECEIVED))
            prStatus = PRStatus.DOCUMENT_REVIEWED;
        else
            throw new PRServerException("Invalid PR status! Please check.");

        try {

            newPRState = new PRState(previousPrState.get(0).getState().getData(), prStatus);

            FlowHandle<SignedTransaction> signedTransactionFlowHandle = connector.getRPCops().startFlowDynamic(
                    WesInitiator.class,
                    new PRFlowData(newPRState, previousPrState.get(0), command));
            SignedTransaction signedTransaction = signedTransactionFlowHandle.getReturnValue().get();

            return ResponseEntity.ok("PRState updated successfully with Txn ID: " + signedTransaction.getId().toString());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

    }

    /**
     * @param requestId  is UUID which helps to update the state
     * @param ecaStateBO is a json object which we provide as an input to our post api
     * @return It returns status whether PR request is updated or not
     * @throws Exception
     */
    @CrossOrigin
    @PostMapping("/{requestId}")
    public ResponseEntity addECAReport(@PathVariable(value = "requestId", required = true) String requestId, @RequestBody ECAStateBO ecaStateBO) throws Exception {

        if (StringUtils.isEmpty(requestId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid requestId!");
        }
        PRState newPRState = null;
        PRContract.Commands command = new PRContract.Commands.RequestApproval();
        PRStatus prStatus = null;

        List<StateAndRef<PRState>> previousPrState = PRControllerHelper.getPrStateFromRequestId(requestId, connector.getRPCops());

        if (previousPrState == null || previousPrState.isEmpty()) {
            throw new PRException("PR Request with id: " + requestId + " doesn't exist please verify and try again!");
        }

        if (previousPrState.get(0).getState().getData().getPrStatus().equals(PRStatus.DOCUMENT_REVIEWED))
            prStatus = PRStatus.ECA_REPORT_CREATED;
        else
            throw new PRServerException("Invalid PR status! Please check.");

        try {

            ECAState ecaState = convertToECAState(ecaStateBO);

            if (ecaState != null)
                newPRState = new PRState(previousPrState.get(0).getState().getData(), ecaState, prStatus);

            FlowHandle<SignedTransaction> signedTransactionFlowHandle = connector.getRPCops().startFlowDynamic(
                    WesInitiator.class,
                    new PRFlowData(newPRState, previousPrState.get(0), command));
            SignedTransaction signedTransaction = signedTransactionFlowHandle.getReturnValue().get();

            return ResponseEntity.ok("ECA Report created successfully with Txn ID: " + signedTransaction.getId().toString());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

    }

    /**
     * @param id is a UUID wesReferenceNumber which should be provided while querying state according to wesReferenceNumber
     * @return It returns the state by querying the vault
     * @throws Exception
     */

    @CrossOrigin
    @GetMapping("/{id}")
    public ResponseEntity getPRRequestDetails(@PathVariable(value = "id", required = false) String id) throws Exception {

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

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new ResponseEntity<>("", HttpStatus.BAD_REQUEST);
    }


}