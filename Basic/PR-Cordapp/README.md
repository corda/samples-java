<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# PR - CorDapp - Java

Welcome to the PR - CorDapp. This CorDapp is one of the module for Permanent Relocation process (PR - Process). There are 3 parties which are in scope of this usecase - Consultant, Wes and University.


Entities (Nodes):

* **Consultant**: The consultant acts on behalf of applicant to raise a PR- Request. A consultant can have multiple applicants for PR. 
* **Wes**: It is a non-profit organisation that provides credential evaluations for international students and immigrants planning to study or work in the U.S. and Canada. Founded in 1974, it is based in New York, U.S. It also has operations in Toronto, Canada.
* **University**: It sends the transcripts of applicant to WES.

Below steps are involved in PR - Cordapp.

Every PR applicant (Consultant)  has to get their transcripts  assessed 


### Business Flow without Corda:

1.    Consultant creates an account in WES and pays the fee.
2.    WES gives a Wes reference number back to Consultant.
3.    Consultant fills a form and visit the University manually.
4.    Consultant submits a form to collect the transcript.  (Time-period 25-30 days)
5.    Once the transcripts are available then consultant collects them again by visiting the University, submit the Wes form , attest a degree certificate copy from University and then use the University's postal service to send the sealed envelope carrying all the documents to WES office in Canada. (Time-period 10 days)
6.    Once the document is received by WES it takes another month for them to verify and evaluate your transcripts and then generate an ECA report.

### Pain-points:

1.    No trust between parties - There are cases reported of forged transcripts which led to use University postal service compulsorily to send transcripts to WES.
2.    Duration - The total time taken in process is more than 2 months.
3.    Expensive - Sending documents to and forth is quite expensive.
4.    Manual process


### How Corda helps?

1.    By making the Consultant, Wes and University part of the same network we make the process transparent.
2.    No need for postal service and sealed or stamped documents to be sent to WES.
3.    Real time submission and verification of transcripts and ECA report generation.
4.    Reduces time by a huge margin.
5.    Creates trust between all parties.
6.    Dematerialisation of transcripts which makes it easily verifiable as the transaction would be signed by University. 
7.    Streamlining the entire process.
8.    Real time updates to all parties.
9.    Making the process online.



Our use-case tries to help people in the PR process by reducing the cost ,time and manual efforts entirely. This also reduces chances of forgery of documents and create a sense of trust among all parties.

This Cordapp has developed in latest Cordapp version 4.0 and it shows how to develop a complete use-case using REST Api's and multi module gradle project.
Please take a look at CreateServer task in main build.gradle which automates the web-server execution.


# Pre-Requisites

 * Install Java 8
 * Install IntelliJ IDEA
 * Install Postman


# Running the PR - CorDapp

### First Way:

**Commands:**
```
 * gradlew clean build
 * gradlew clean deployNodes
```
 
 Navigate to `/build/nodes` folder 
```
   * cd build/nodes
   * runnodes.bat 
```
  
 
 **Running the Web-Servers:**
 
 There are 3 web-servers for 3 nodes. Open separate consoles for each web-server.
 
 **Commands:**
 
 ```
   * gradlew runConsultantServer
   * gradlew runWesServer
   * gradlew runUniversityServer
 ```
 
 
 
 
 
### Second Way:
 
 **Commands:**
  ```
    * gradlew clean build
    * gradlew clean deployNodes
    * gradlew createServer
  ```
 
  
Navigate to `/build/nodes` folder 

```
    * cd build/nodes
    * runnodes.bat
    * runserver.bat
  ```   
    "runserver.bat" file takes care of running all the webservers.

 ## PR-Cordapp Flow
 
 <img width="1338" alt="PR-Cordapp Flow" src="https://user-images.githubusercontent.com/35623981/82752830-6e79d480-9dde-11ea-89f3-a2078e263f14.png">

 
 ## Running API's
 
 Please import api's from below link in postman.
 
  `https://www.getpostman.com/collections/5b924208b191625557d9`
 
  * Send PR request from Consultant to Wes. One can update json body. **(SendPRRequest)**  
      `http://localhost:8081/consultant/`
  
  * Get the PR request created and copy `wesReferenceNumber (Id)` from response. One can run same api on Consultant and Wes.  **(GetPRRequest)**
    `http://localhost:8081/consultant/`
    
  * Respond to PR request created from Wes. **(SendResponseToPRRequest)**
    `http://localhost:8083/wes/{wesReferenceId}`
    
  * Raise request for student transcript and add `wesReferenceId` in json body. **(RaiseStudentAcademicTranscriptRequest)**
    `http://localhost:8081/consultant/transcript/CREATE`
    
  * Get students transcript request details and copy `requestId` from response. **(GetAllStudentsAcademicTranscriptRequestDetails)**
    `http://localhost:8081/consultant/transcript/1` 
    
  * Add transcript details by University for a particular requestId. **(AddTranscriptDetails)**
    `http://localhost:8082/university/transcript/{requestId}`
    
  * Confirm details of a transcript request. **(ConfirmTranscriptDetails)**
    `http://localhost:8081/consultant/transcript/{requestId}`
    
  * Send confirmed transcripts to Wes from University. **(ReadyForWES)**
    `http://localhost:8082/university/transcript/{requestId}` 
    
  * Get updated transcript details at Wes. **(GetAllStudentsAcademicTranscriptRequestDetails)**
    `http://localhost:8082/university/transcript/1`
    
  * Change PR Request's `prStatus` from `APPLICATION_ACKNOWLEDGEMENT` to `DOCUMENT_RECEIVED`. **(SendResponseToPRRequest)**
    `http://localhost:8083/wes/{wesReferenceId}`
    
  * Change PR Request's `prStatus` from `DOCUMENT_RECEIVED` to `DOCUMENT_REVIEWED`. **(SendResponseToPRRequest)**
      `http://localhost:8083/wes/{wesReferenceId}`
    
  * Send ECA details to PR request **(SendECAResponseToPRRequest)**
    `http://localhost:8083/wes/{wesReferenceId}`
    
  * ECA report is created and added to PR-Request. **(GetPRRequest)**
    `http://localhost:8081/consultant/`


## Interacting with the nodes

### Shell

When started via the command line, each node will display an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.
    
    Sun May 24 15:06:30 IST 2020>>>

You can use this shell to interact with your node. For example, enter `run networkMapSnapshot` to see a list of 
the other nodes on the network:

    Sun May 24 15:12:51 IST 2020>>> run networkMapSnapshot
    - addresses:
      - "localhost:10005"
      legalIdentitiesAndCerts:
      - "O=Consultants, L=London, C=GB"
      platformVersion: 4
      serial: 1590312506047
    - addresses:
      - "localhost:10008"
      legalIdentitiesAndCerts:
      - "O=University, L=New York, C=US"
      platformVersion: 4
      serial: 1590312514588
    - addresses:
      - "localhost:10011"
      legalIdentitiesAndCerts:
      - "O=Wes, L=London, C=GB"
      platformVersion: 4
      serial: 1590312504774
    - addresses:
      - "localhost:10002"
      legalIdentitiesAndCerts:
      - "O=Notary, L=London, C=GB"
      platformVersion: 4
      serial: 1590312518846


You can find out more about the node shell [here](https://docs.corda.net/shell.html).


### Developers:
* [Rishi Kundu](https://www.linkedin.com/in/rishi-kundu-1990/)
* [Ajinkya Pande](https://www.linkedin.com/in/ajinkya-pande-013ab5106/)

##### Please feel free to raise a pull request.
