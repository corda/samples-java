<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Example CorDapp [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Basic/cordapp-example)

Welcome to the example CorDapp. This CorDapp is fully documented [here](http://docs.corda.net/tutorial-cordapp.html).

This example application has modules for both Java and Kotlin and is an exploratory sample for the official [Corda online training](https://training.corda.net). It demonstrates the basic components present in a CorDapp and how they work together using a simple IOU example.

Additional Notes:

With Corda release 4.6, open source cordapps will have to write liquibase scripts to generate the custom database tables. 
For rapid development in dev mode with H2, you could use below command to generate Hibernate entities for your custom schemas. You will not 
require Liquibase scripts in such a situation.

    java -jar corda.jar run-migration-scripts --app-schemas

Liquibase scripts have been added to workflow-java/src/main/resojurces/migration folder.

To run the migration scripts for corda node use below command

    java -jar corda.jar run-migration-scripts --core-schemas


To run the migration scripts for your custom tables defined in your Cordapp use below command

    java -jar corda.jar run-migration-scripts --app-schemas

If you already have hibernate entities created in your db, prior to using Corda version 4.6, use below command to sync the database

    java -jar corda.jar sync-app-schemas
    
    
Read more about hwo to add liquibase to your cordapp here.
#  TODO update documentation link.
