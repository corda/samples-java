package net.corda.samples.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.contracts.asset.Cash;
import net.corda.samples.flows.IOUIssueFlow;
import net.corda.samples.flows.IOUSettleFlow;
import net.corda.samples.flows.IOUTransferFlow;
import net.corda.samples.flows.SelfIssueCashFlow;
import net.corda.samples.states.IOUState;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.corda.finance.workflows.GetBalances.getCashBalances;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/iou") // The paths for HTTP requests are relative to this base path.
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    public MainController(NodeRPCConnection rpc) {
        this.proxy = rpc.getProxy();
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();

    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name){
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities()
                .stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

    @GetMapping(value = "/status", produces = TEXT_PLAIN_VALUE)
    private String status() {
        return "200";
    }

    @GetMapping(value = "/servertime", produces = TEXT_PLAIN_VALUE)
    private String serverTime() {
        return (LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC"))).toString();
    }

    @GetMapping(value = "/addresses", produces = TEXT_PLAIN_VALUE)
    private String addresses() {
        return proxy.nodeInfo().getAddresses().toString();
    }

    @GetMapping(value = "/identities", produces = TEXT_PLAIN_VALUE)
    private String identities() {
        return proxy.nodeInfo().getLegalIdentities().toString();
    }

    @GetMapping(value = "/platformversion", produces = TEXT_PLAIN_VALUE)
    private String platformVersion() {
        return Integer.toString(proxy.nodeInfo().getPlatformVersion());
    }

    @GetMapping(value = "/peers", produces = APPLICATION_JSON_VALUE)
    public HashMap<String, List<String>> getPeers() {
        HashMap<String, List<String>> myMap = new HashMap<>();

        // Find all nodes that are not notaries, ourself, or the network map.
        Stream<NodeInfo> filteredNodes = proxy.networkMapSnapshot().stream()
                .filter(el -> !isNotary(el) && !isMe(el) && !isNetworkMap(el));
        // Get their names as strings
        List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
                .collect(Collectors.toList());

        myMap.put("peers", nodeNames);
        return myMap;
    }

    @GetMapping(value = "/notaries", produces = TEXT_PLAIN_VALUE)
    private String notaries() {
        return proxy.notaryIdentities().toString();
    }

    @GetMapping(value = "/flows", produces = TEXT_PLAIN_VALUE)
    private String flows() {
        return proxy.registeredFlows().toString();
    }

    @GetMapping(value = "/states", produces = TEXT_PLAIN_VALUE)
    private String states() {
        return proxy.vaultQuery(ContractState.class).getStates().toString();
    }

    @GetMapping(value = "/me",produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami(){
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
    }
    @GetMapping(value = "/ious",produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<IOUState>> getIOUs() {
        // Filter by state type: IOU.
        return proxy.vaultQuery(IOUState.class).getStates();
    }
    @GetMapping(value = "/cash",produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<Cash.State>> getCash() {
        // Filter by state type: Cash.
        return proxy.vaultQuery(Cash.State.class).getStates();
    }

    @GetMapping(value = "/cash-balances",produces = APPLICATION_JSON_VALUE)
    public Map<Currency,Amount<Currency>> cashBalances(){
        return getCashBalances(proxy);
    }

    @PutMapping(value =  "/issue-iou" , produces = TEXT_PLAIN_VALUE )
    public ResponseEntity<String> issueIOU(@RequestParam(value = "amount") int amount,
                                           @RequestParam(value = "currency") String currency,
                                           @RequestParam(value = "party") String party) throws IllegalArgumentException {
        // Get party objects for myself and the counterparty.
        Party me = proxy.nodeInfo().getLegalIdentities().get(0);
        Party lender = Optional.ofNullable(proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(party))).orElseThrow(() -> new IllegalArgumentException("Unknown party name."));
        // Create a new IOU state using the parameters given.
        try {
            IOUState state = new IOUState(new Amount<>((long) amount * 100, Currency.getInstance(currency)), lender, me);
            // Start the IOUIssueFlow. We block and waits for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(IOUIssueFlow.InitiatorFlow.class, state).getReturnValue().get();
            // Return the response.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id "+ result.getId() +" committed to ledger.\n " + result.getTx().getOutput(0));
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
    @GetMapping(value =  "transfer-iou" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> transferIOU(@RequestParam(value = "id") String id,
                                              @RequestParam(value = "party") String party) {
        UniqueIdentifier linearId = new UniqueIdentifier(null,UUID.fromString(id));
        Party newLender = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(party));
        try {
            proxy.startTrackedFlowDynamic(IOUTransferFlow.InitiatorFlow.class, linearId, newLender).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body("IOU "+linearId.toString()+" transferred to "+party+".");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Settles an IOU. Requires cash in the right currency to be able to settle.
     * Example request:
     * curl -X GET 'http://localhost:10007/api/iou/settle-iou?id=705dc5c5-44da-4006-a55b-e29f78955089&amount=98&currency=USD'
     */
    @GetMapping(value =  "settle-iou" , produces = TEXT_PLAIN_VALUE )
    public  ResponseEntity<String> settleIOU(@RequestParam(value = "id") String id,
                                             @RequestParam(value = "amount") int amount,
                                             @RequestParam(value = "currency") String currency) {

        UniqueIdentifier linearId = new UniqueIdentifier(null, UUID.fromString(id));
        try {
            proxy.startTrackedFlowDynamic(IOUSettleFlow.InitiatorFlow.class, linearId,
                    new Amount<>((long) amount * 100, Currency.getInstance(currency))).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(""+ amount+ currency +" paid off on IOU id "+linearId.toString()+".");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Helper end-point to issue some cash to ourselves.
     * Example request:
     * curl -X GET 'http://localhost:10009/api/iou/self-issue-cash?amount=100&currency=USD'
     */
    @GetMapping(value =  "self-issue-cash" , produces =  TEXT_PLAIN_VALUE )
    public ResponseEntity<String> selfIssueCash(@RequestParam(value = "amount") int amount,
                      @RequestParam(value = "currency") String currency) {

        try {
            Cash.State cashState = proxy.startTrackedFlowDynamic(SelfIssueCashFlow.class,
                    new Amount<>((long) amount * 100, Currency.getInstance(currency))).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(cashState.toString());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}