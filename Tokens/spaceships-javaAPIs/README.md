# spaceships-javaAPIs token sample cordapp [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Tokens/spaceships-javaAPIs)

## IMPORTANT: this project is using TokenSDK Pre-signed developer snapshot pending the formal release. NOT FOR PRODUCTION. This repo will be updated when available.

```java
tokens_release_group = 'com.r3.corda.lib.tokens'
tokens_release_version = '1.2-RC02-PRESIGN'
```

---
This CorDapp demonstrates the new Java APIs released with [TokenSDK](https://training.corda.net/libraries/tokens-sdk/) 1.2

For an exploratory overview of usage, checkout the following blog post: [here](https://medium.com/corda/corda-tokens-made-easy-with-new-java-apis-83095693d72)

The core changes include easier access to the following Utility classes and functions from Java:
- com.r3.corda.lib.tokens.money
  - MoneyUtilities
  - DigitalCurrency
  - FiatCurrency 
- com.r3.corda.lib.tokens.selection
  - SelectionUtilities
  - TokenQueryBy
- com.r3.corda.lib.tokens.selection.database.config
  - DatabaseSelectionConfig
- com.r3.corda.lib.tokens.selection.database.selector
  - DatabaseTokenSelection
- com.r3.corda.lib.tokens.contracts.utilities
  - AmountUtilities
- com.r3.corda.lib.tokens.workflows.utilities
  - FlowUtilities
  - NotaryUtilities
  - QueryUtilities
  - TokenUtilities
  
Additionally two new Java Builder classes have been added to allow easy creation of both [Fungible](https://training.corda.net/libraries/tokens-sdk/#fungibletoken) and [NonFungible](https://training.corda.net/libraries/tokens-sdk/#nonfungibletoken) Tokens:
- FungibleTokenBuilder
- NonFungibleTokenBuilder

The CorDapp will allow International Planetary Council (IPC) residents to use their local currencies to either purchase unique spaceships (represented by NonFungibleToken) OR invest in partial ownership of a spaceship (represented by a FungibleToken). 

Examples of the new Java APIs will be used throughout and identified as such.



## Concepts


### Flows

Flows are executed through the `FlowTests` class in the workflows module. They can also be run through the [CRaSH](https://docs.corda.net/docs/corda-os/shell.html) shell.


## Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

For a brief introduction to Token SDK in Corda, see https://medium.com/corda/introduction-to-token-sdk-in-corda-9b4dbcf71025

## Usage

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

#### Shell

When started via the command line, each node will display an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.

    Tue July 09 11:58:13 GMT 2019>>>

You can use this shell to interact with your node.

