# queryablestate cordapp [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Features/queryablestate-carinsurance)

This CorDapp demonstrates [QueryableState](https://docs.corda.net/docs/corda-os/api-persistence.html) works in Corda. Corda allows developers
to have the ability to expose some or all parts of their states to a custom database
table using an ORM tools. To support this feature the state must implement
`QueryableState`.

In this CorDapp we would use an `Insurance` state and persist its properties in a
custom table in the database.  The `Insurance` state among other fields also
contains an `VehicleDetail` object, which is the asset being insured. We have used
this `VehicleDetail` to demonstrate _One-to-One_ relationship. Similarly we also
have a list of `Claim` objects in the `Insurance` state which represents claims
made against the insurance. We use them to demonstrate _One-to-Many_ relationship.



## Concepts

A spring boot client is provided with the cordapp, which exposes two REST endpoints
(see `Controller` in the clients module) to trigger the flows.
Use the command `./gradlew bootRun` in the project root folder to run the [Spring Boot
Server](https://spring.io/projects/spring-boot#overview).

### Flows

There are two flow in this cordapp:

1. [IssueInsuranceInitiator](./workflows/src/main/java/net/corda/examples/carinsurance/flows/IssueInsuranceFlow.java#L39-L67): It creates the insurance state with the associated vehicle
information.

2. [InsuranceClaimInitiator](./workflows/src/main/java/net/corda/examples/carinsurance/flows/InsuranceClaimFlow.java#L34-L57): It creates the claims against the insurance.


## Usage

### Pre-requisites:
See https://docs.corda.net/getting-set-up.html.

### Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```

### Interacting with the nodes

The Postman collection containing API's calls to the REST endpoints can be imported
from the link: https://www.getpostman.com/collections/ddc01c13b8ab4b5e853b.
Use the option Import > Import from Link option in Postman to import the collection.

<p align="center">
<img src="./clients/src/main/resources/static/Postman_screenshot.png" alt="Postman Import Collection" width="400">
</p>


### Connecting to the Database

The JDBC url to connect to the database would be printed in the console in node
startup. Use the url to connect to the database using a suitable client. The
default username is 'sa' and password is '' (blank).
You could download H2 Console to connect to h2 database here:
http://www.h2database.com/html/download.html

<p align="center">
  <img src="./clients/src/main/resources/static/JDBC-url.png" alt="Database URL" width="400">
</p>

Refer here for more details regarding connecting to the node database.
https://docs.corda.net/head/node-database-access-h2.html
