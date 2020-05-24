package com.pr.server.common.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pr.contract.state.schema.schema.PRSchemaV1;
import com.pr.contract.state.schema.states.PRState;
import com.pr.student.contract.state.schema.schema.RequestFormSchemaV1;
import com.pr.student.contract.state.schema.state.RequestForm;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.serialization.CordaSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_NUM;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_SIZE;


/**
 * @author Ajinkya Pande & Rishi Kundu
 */

@CordaSerializable
public class PRControllerHelper {

    private final static Logger logger = LoggerFactory.getLogger(PRControllerHelper.class);

    /**
     * View PrRequest by requestId
     */

    public static List<StateAndRef<PRState>> getPrStateFromRequestId(String requestId, CordaRPCOps rpcOps)
            throws NoSuchFieldException {


        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        Field PrIdField = PRSchemaV1.PersistentPR.class.getDeclaredField("wesReferenceNumber");

        CriteriaExpression PrIdIndex = Builder.equal(PrIdField, requestId);
        QueryCriteria customCriteria = new QueryCriteria.VaultCustomQueryCriteria<>(PrIdIndex);
        QueryCriteria criteria = generalCriteria.and(customCriteria);
        PageSpecification pageSpecification;
        Vault.Page<PRState> results;
        List<StateAndRef<PRState>> states = new ArrayList<>();
        Integer pageNum = DEFAULT_PAGE_NUM;
        do {
            pageSpecification = new PageSpecification(pageNum, DEFAULT_PAGE_SIZE);
            results = rpcOps.vaultQueryByWithPagingSpec(PRState.class, criteria, pageSpecification);
            states.addAll(results.getStates());
            pageNum++;
        } while ((pageSpecification.getPageSize() * (pageNum)) <= results.getTotalStatesAvailable());
        if (results.getStates().size() > 0) {
            return results.getStates();

        } else {
            return new ArrayList<>();
        }

    }

    public static List<StateAndRef<RequestForm>> getRequestFormStateFromRequestId(String requestId, CordaRPCOps rpcOps)
            throws NoSuchFieldException {


        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        Field reqIdField = RequestFormSchemaV1.PersistentRequestForm.class.getDeclaredField("wes_reference_number");

        CriteriaExpression reqIdIndex = Builder.equal(reqIdField, requestId);
        QueryCriteria customCriteria = new QueryCriteria.VaultCustomQueryCriteria<>(reqIdIndex);
        QueryCriteria criteria = generalCriteria.and(customCriteria);
        PageSpecification pageSpecification;
        Vault.Page<RequestForm> results;
        List<StateAndRef<RequestForm>> states = new ArrayList<>();
        Integer pageNum = DEFAULT_PAGE_NUM;
        do {
            pageSpecification = new PageSpecification(pageNum, DEFAULT_PAGE_SIZE);
            results = rpcOps.vaultQueryByWithPagingSpec(RequestForm.class, criteria, pageSpecification);
            states.addAll(results.getStates());
            pageNum++;
        } while ((pageSpecification.getPageSize() * (pageNum)) <= results.getTotalStatesAvailable());
        if (results.getStates().size() > 0) {
            return results.getStates();

        } else {
            return new ArrayList<>();
        }

    }

    public static List<StateAndRef<RequestForm>> createOutputWithHash(List<StateAndRef<RequestForm>> requestedStates)
            throws JsonProcessingException {

        for (StateAndRef<RequestForm> requestStatePage : requestedStates) {
            String stateHash = requestStatePage.getRef().getTxhash().toString();
            requestStatePage.getState().getData().setStateHash(stateHash);
        }

        return requestedStates;
    }

    public static List<StateAndRef<RequestForm>> getRequestFormStateFromLinearId(String id, CordaRPCOps rpcOps) {
        UniqueIdentifier uniqueIdentifier = UniqueIdentifier.Companion.fromString(id);

        Set<Class<RequestForm>> contractStateTypes = new HashSet(Collections.singletonList(RequestForm.class));
        List<StateAndRef<RequestForm>> updatedStateList;

        QueryCriteria linearCriteria = new QueryCriteria.LinearStateQueryCriteria(null, Arrays.asList(uniqueIdentifier),
                Vault.StateStatus.UNCONSUMED, contractStateTypes);

        Vault.Page<RequestForm> results = rpcOps.vaultQueryByCriteria(linearCriteria, RequestForm.class);

        if (results.getStates().size() > 0) {
            return results.getStates();
        } else {
            return new ArrayList<>();
        }
    }

}
