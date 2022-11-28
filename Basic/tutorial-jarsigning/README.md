# Network Bootstrapper Tutorial

This tutorial demonstrates how to sign a contract jar with your own keystore. 

A CorDapp will most likely have two jars, one contract jar and one workflow jar. Since all the data and transactional rules are defined in the contract, when transacting over the Corda Network, we will need to check the hashes of the contract jars. Hence, when speaking of signing a CorDapp, we are most likely talking about signing the contract jar. 

The signing option is defined in the build.gradle file of the /workflows and /contracts folder. 
```
cordapp {
    targetPlatformVersion corda_platform_version
    minimumPlatformVersion corda_platform_version
    workflow {
        name "4.8LTS Tutorial Flows"
        vendor "Corda Open Source"
        licence "Apache License, Version 2.0"
        versionId 1
    }
    signing {
        enabled false
    }
}
```
In this example, we disable the signing for the workflow jar. And, for the contract jar, we will add custom keystore to use for signing. 
```
cordapp {
    targetPlatformVersion corda_platform_version
    minimumPlatformVersion corda_platform_version
    contract {
        name "4.8LTS Tutorial Contracts"
        vendor "Corda Open Source"
        licence "Apache License, Version 2.0"
        versionId 1
    }
    signing {
        enabled true
        options {
            Properties constants = new Properties()
            file("$projectDir/../gradle.properties").withInputStream { constants.load(it) }
            keystore getProperty('jar.sign.keystore')
            alias "cordapp-signer"
            storepass getProperty('jar.sign.password')
            keypass getProperty('jar.sign.password')
            storetype "PKCS12"
        }
    }
}
```
In the terminal, create a private key in JKS format (replace the X500 name with yours, and use the same [password] value for both storepass and keypass):

```
keytool -keystore jarSignKeystore.jks -keyalg RSA -genkey -dname "OU=, O=, L=, C=" -storepass [password] -keypass [password] -alias cordapp-signer
```

Migrate the JKS key to PKCS12 format. You will be prompted for 2 passwords, use the same value that you used to create the JKS key for both values:

```
keytool -importkeystore -srckeystore jarSignKeystore.jks -destkeystore jarSignKeystore.pkcs12 -deststoretype pkcs12
```

As you can see, we are importing in variables from the gradle.properties file in the root directory. They are the keystore path and the password for the keystore
```
jar.sign.keystore = path to PKCS12 file
jar.sign.password = password of PKCS12 file
```
Once you have edited all the above fields, you can simply gradle task build to execute the building and signing of the jar. 
```
./gradlew build
```
You will have a signed jar with your keystore. 
