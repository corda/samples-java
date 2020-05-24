package com.pr.webserver.controller;

import com.pr.common.data.RequestFlowData;
import com.pr.server.common.bo.impl.*;
import com.pr.server.common.controller.CommonController;
import com.pr.server.common.exception.PRServerException;
import com.pr.server.common.helper.PRControllerHelper;
import com.pr.student.contract.state.schema.contract.RequestFormContract;
import com.pr.student.contract.state.schema.state.*;
import com.pr.university.initiator.RequestFlowResponseInitiator;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

@RestController
@RequestMapping("/university") // The paths for HTTP requests are relative to this base path.
@CrossOrigin
public class UniversityController extends CommonController {

    private final static Logger logger = LoggerFactory.getLogger(UniversityController.class);


    @CrossOrigin
    @GetMapping(value = "/hello", produces = "text/plain")
    private String uniName() {
        return "Hello University";
    }

    /**
     *
     * @param requestId is a UUID which helps to query the state from vault
     * @param requestStatus is a parameter to change request status (APPLICATION_READY_FOR_WES_VERIFICATION etc.)
     * @return It returns status whether Transaction is successful or not
     */

    @CrossOrigin
    @PutMapping("transcript/{requestId}")
    public ResponseEntity TranscriptDetailsReadyForWES(@PathVariable("requestId") String requestId,
                                                       @RequestParam(value = "requestStatus", required = false,
                                                               defaultValue = "APPLICATION_READY_FOR_WES_VERIFICATION") String requestStatus) {
        RequestFormContract.Commands contractCommand;
        SignedTransaction signedTransaction;

        if (StringUtils.isEmpty(requestId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("RequestId! is missing. Please enter a valid booking Id.");
        }

        contractCommand = new RequestFormContract.Commands.UPDATE();

        List<StateAndRef<RequestForm>> previousRequest = PRControllerHelper.getRequestFormStateFromLinearId(requestId, connector.getRPCops());

        if (previousRequest == null || previousRequest.isEmpty()) {
            throw new PRServerException("Request Id with id: " + requestId + " doesn't exist please verify and try again!");
        }

        if (!previousRequest.get(0).getState().getData().getRequestStatus().equals(RequestStatus.CONFIRMED)) {
            throw new IllegalArgumentException("Invalid request status, Expected Previous Status : " + RequestStatus.CONFIRMED);
        }

        RequestForm newRequestFormState = new RequestForm(previousRequest.get(0).getState().getData(), RequestStatus.APPLICATION_READY_FOR_WES_VERIFICATION);

        try {
            FlowHandle<SignedTransaction> signedTransactionFlowHandle = connector.getRPCops().startFlowDynamic(RequestFlowResponseInitiator.class,
                    new RequestFlowData(newRequestFormState, previousRequest.get(0), contractCommand));

            signedTransaction = signedTransactionFlowHandle.getReturnValue().get();
            return ResponseEntity.ok("Transcript details ready for WES verification. Txn ID: " + signedTransaction.getId().toString());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

    }

    /***
     *
     * @param requestId is a UUID which helps to query the state from vault
     * @param studentInfoBO is a json object which we provide as an input to our post api
     * @param requestStatus is a parameter to change request status (ADDED_TRANSCRIPT_DETAILS etc.)
     * @return It returns status whether Transcript details are added or not
     */

    @CrossOrigin
    @PostMapping("transcript/{requestId}")
    public ResponseEntity addTranscriptDetails(@PathVariable("requestId") String requestId,
                                               @RequestBody StudentInfoBO studentInfoBO,
                                               @RequestParam(value = "requestStatus", required = false,
                                                       defaultValue = "ADDED_TRANSCRIPT_DETAILS") String requestStatus) {

        RequestFormContract.Commands contractCommand;
        SignedTransaction signedTransaction;
        DegreeDetailsBO degreeDetailsBO = null;
        List<Semester> semesterList = new ArrayList<>();
        List<SemesterBO> semesterBOList = new ArrayList<>();
        DegreeDetails degreeDetails=null;
        University university=null;
        UniversityBO universityBO = null;
        TranscriptBO transcriptBO = null;
        Transcript transcript = null;
        Semester semester = null;

        if (StringUtils.isEmpty(requestId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("RequestId! is missing. Please enter a valid Id.");
        }

        contractCommand = new RequestFormContract.Commands.UPDATE();

        List<StateAndRef<RequestForm>> previousRequest = PRControllerHelper.getRequestFormStateFromLinearId(requestId, connector.getRPCops());

        if (previousRequest == null || previousRequest.isEmpty()) {
            throw new PRServerException("Request Id with id: " + requestId + " doesn't exist please verify and try again!");
        }

        if (!previousRequest.get(0).getState().getData().getRequestStatus().equals(RequestStatus.APPLICATION_SUBMITTED)) {
            throw new IllegalArgumentException("Invalid request status, Expected Previous Status : " + RequestStatus.APPLICATION_SUBMITTED);
        }

        if (studentInfoBO.getDegreeDetailsBO()==null && studentInfoBO.getTranscriptBO()==null && studentInfoBO.getUniversityBO()==null){
            throw new PRServerException("Please provide relevant student data. Please check.");
        }


        if (studentInfoBO.getDegreeDetailsBO()!=null) {
            degreeDetailsBO = studentInfoBO.getDegreeDetailsBO();
            degreeDetails = new DegreeDetails(previousRequest.get(0).getState().getData().getDegreeName(),
                    previousRequest.get(0).getState().getData().getUniversityName(), studentInfoBO.getDegreeStatus(),
                    degreeDetailsBO.getPassingYear(), degreeDetailsBO.getPassingDivision(),
                    degreeDetailsBO.getFullName(), degreeDetailsBO.getFatherName(), degreeDetailsBO.getSpecializationField(),
                    previousRequest.get(0).getState().getData().getRollNumber());
        }
        if (studentInfoBO.getUniversityBO()!=null) {
            universityBO = studentInfoBO.getUniversityBO();
            university = new University(previousRequest.get(0).getState().getData().getUniversityName(),
                    previousRequest.get(0).getState().getData().getUniversityAddress(), universityBO.getUniversityType(),
                    universityBO.getContactNumber());
        }

        if (studentInfoBO.getTranscriptBO()!=null) {
            transcriptBO = studentInfoBO.getTranscriptBO();
            semesterBOList = transcriptBO.getSemester();

            if (!semesterBOList.isEmpty()) {
                for (SemesterBO semesterBO : semesterBOList) {
                    List<Subjects> subjectsList = new ArrayList<>();
                    for (SubjectBO subjectBO : semesterBO.getSubbjectsList()) {
                        Subjects subjects = new Subjects(subjectBO.getSubjectName(), subjectBO.getMarksObtained());
                        logger.info(subjects.toString());
                        subjectsList.add(subjects);
                    }
                    semester = new Semester(semesterBO.getSemesterNumber(), subjectsList, semesterBO.getResultDeclaredOnDate());
                    semesterList.add(semester);
                }
            }
            transcript = new Transcript(previousRequest.get(0).getState().getData().getRollNumber(), previousRequest.get(0).getState().getData().getStudentName(),
                    previousRequest.get(0).getState().getData().getUniversityName(), previousRequest.get(0).getState().getData().getDegreeName(),
                    semesterList);
            logger.info(transcript.toString());
        }

        StudentInfoState studentInfoState = new StudentInfoState(previousRequest.get(0).getState().getData().getRollNumber(),
                studentInfoBO.getCourseDuration(), studentInfoBO.getDegreeStatus(), degreeDetails,transcript,university);

        RequestForm newRequestFormState = new RequestForm(previousRequest.get(0).getState().getData(), studentInfoState,RequestStatus.ADDED_TRANSCRIPT_DETAILS);

        try {
            FlowHandle<SignedTransaction> signedTransactionFlowHandle = connector.getRPCops().startFlowDynamic(RequestFlowResponseInitiator.class,
                    new RequestFlowData(newRequestFormState, previousRequest.get(0), contractCommand));

            signedTransaction = signedTransactionFlowHandle.getReturnValue().get();
            return ResponseEntity.ok("Transcript details added for confirmation with Txn ID: " + signedTransaction.getId().toString());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

    }
}