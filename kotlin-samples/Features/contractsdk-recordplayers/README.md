# Contract SDK - Record Player Maintenance


If you're familiar with record players you probably know how difficult it is to make sure that they run in pristine condition, especially if it's a collectible.

![](./cordaphone.png)


This cordapp simulates how you could model the process of a limited edition record player (the cordagraf) that is manufactured and issued to specific dealers, and those dealers are the only entities that can service those record players after the fact and report stats back to the manufacturer about how the players are being used.

A Record Player is issued as a [LinearState](https://docs.r3.com/en/platform/corda/4.9/community/api-states.html#linearstate) and is updated by dealers, that act functionally as the only entity that can update the `RecordPlayerState`.


### Using the Contract SDK

The [Contract SDK](https://github.com/corda/contract-sdk) is a series of annotations that you can use in your Corda contracts. It's great for putting together cleaner contract code and writing it faster.

This repository demonstrates how you can configure and use it in your own projects.

Configuration requires three steps:

1. Adding `maven { url 'https://software.r3.com/artifactory/corda-lib-dev' }` to `repositories.gradle`
2. Adding `compile "com.r3.corda.lib.contracts:contract-sdk:0.9-SNAPSHOT"` to the `build.gradle` file of your contract module in your cordapp 
3. Adding the annotations to your CorDapp


To show how convenient this can be, here's an example demonstrating how to configure a simple issuance command with one output and no inputs.

```kotlin
// note the annotations here from the Contracts SDK
@RequireNumberOfStatesOnInput(value = 0)
@RequireNumberOfStatesOnOutput(value = 1)
class Issue : Commands
```

If you've written contracts before you might be used to outlining the verify method, applying a conditional for issuance commands and configuring a lot of the same verification for contract inputs. This removes the need for those tools.

Take a look at the RecordPlayerContract sample in this repository to see how this works in practice.

## Usage

To run the CorDapp, open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)

```
./gradlew clean build deployNodes
./build/nodes/runnodes
```
In the Manufacturer's interactive shell, type:
```
flow start net.corda.samples.contractsdk.flows.IssueRecordPlayerFlow dealer: "O=Alice Audio,L=New York,C=US", needle: spherical

# you can get your state id with a quick vault query
run vaultQuery contractStateType: net.corda.samples.contractsdk.states.RecordPlayerState
```

```
flow start net.corda.samples.contractsdk.flows.UpdateRecordPlayerFlow stateId: < Place State ID here >, needleId: spherical, magneticStrength: 100, coilTurns: 100, amplifierSNR: 10000, songsPlayed: 100
```

## Additional Information

If you're looking to find more information on record players specifically, I included some sources for how we modeled record players in the state class.

- [You can find much more information on the contract sdk on github here](https://github.com/corda/contract-sdk)
- [The notes on the number of turns in a record player cartridge coil was sourced from here](https://www.vinylengine.com/turntable_forum/viewtopic.php?t=35449)
- [The info on other aspects of amplifiers was sourced from here](https://www.cambridgeaudio.com/usa/en/blog/amplifier-specifications)


