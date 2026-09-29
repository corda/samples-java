# CorDapp Samples - Java
<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

## Introduction
This repository contains multiple sample apps, from CorDapps that help you get started, all the way to demonstrating specific features and advanced usage.

If you are new to Corda and/or would like to learn all of the fundamentals in a guided and incremental manner please visit the Corda [documentation](https://docs.r3.com/) site. 

To get started explore the [Basic](./Basic) folder, or navigate to the [Advanced](./Advanced) and [Features](./Features) folders to see a description of whats available. You can find the exact same set of CorDapp demonstration in Kotlin language at [link](https://github.com/corda/samples-kotlin).

## Building against Corda Enterprise

The samples target Corda Enterprise. Its artifacts (`com.r3.corda:*`) and the R3 libraries they depend on (`com.r3.*`)
are not on any public repository, while the Corda OS artifacts they build on (`net.corda:corda-core`,
`net.corda:corda-finance-contracts`) still come from the public repositories.

Every sample declares its repositories in one place, either its `repositories.gradle` or the `allprojects { repositories { ... } }`
block of its `build.gradle`, and that block ends with a clearly marked **CORDA ENTERPRISE REPOSITORY** entry. That entry is where you
point the build at the repository your organisation serves Corda Enterprise from. As shipped it points at R3's Artifactory
(`https://software.r3.com/artifactory/r3-corda-releases`) and reads the credentials supplied with your licence from the Gradle
properties `cordaArtifactoryUsername` / `cordaArtifactoryPassword` (for example in `~/.gradle/gradle.properties`) or from the
environment variables `CORDA_ARTIFACTORY_USERNAME` / `CORDA_ARTIFACTORY_PASSWORD`. Replace the URL if you mirror the artifacts
internally. Alternatively, install the distribution into your local Maven repository: `mavenLocal()` is searched first.

`SNAPSHOT` versions of Corda Enterprise are not published to `r3-corda-releases`. To build the samples against one, run
`./gradlew publishToMavenLocal` in the Corda Enterprise checkout, or point the entry at your snapshot repository.

The versions and artifact groups are set once per section in its `constants.properties` (`cordaReleaseGroup` for the Enterprise
artifacts, `cordaCoreReleaseGroup` for the Corda OS core).

## Directories
The samples are divided into 5 sections with the following desciption:

* [Accounts](./Accounts): These samples showcases how to utilize Corda Accounts Libray to build CorDapps which aim to have massive user volume
* [Advanced](./Advanced): In these samples, we demonstrate more complex and sophisticated features of Corda.
* [Basic](./Basic): They demonstrate fundamental and useful techniques for CorDapp development.
* [BusinessNetworks](./BusinessNetworks): These include Business Network Extension related samples. Learn more at [bn-extension](https://github.com/corda/bn-extension).
* [Features](./Features): These samples demonstrate specific Corda functionalities.
* [Tokens](./Tokens): These include TokenSDK related samples. Learn more at TokenSDK.
