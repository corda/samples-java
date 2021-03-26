# dockerform-yocordapp

This time we've taken the original yo cordapp and modified it to demonstrate an example of how you can use dockerForm to bootstrap a corda network.

For the purposes of this example, we'll use the yo cordapp as a base to create a clear example for how to use the dockerForm gradle build task in a normal cordapp setup.

> Note this is generally intended to be used on localhost.


## Concepts

In the original yo application, the app sent what is essentially a nudge from one endpoint and another.

In corda, we can use abstractions to accomplish the same thing.


We define a state (the yo to be shared), define a contract (the way to make sure the yo is legit), and define the flow (the control flow of our cordapp).


## Usage

### Quick Start with Docker

If you have docker installed you can use our gradle tasks to generate a valid docker compose file for your node configuration.

```bash
# clone the repository
git clone https://github.com/davidawad/corda-docker-yo-demo && cd corda-docker-yo-demo

# generate the docker-compose file
./gradlew prepareDockerNodes

# run our corda network
docker-compose -f ./build/nodes/docker-compose.yml up
```

#### Sending a Yo

We will interact with the nodes via their specific shells. When the nodes are up and running, use the following command to send a Yo to another node:

```sh
# find the ssh port for PartyA using docker ps
ssh user1@0.0.0.0 -p 2223

# the password defined in the node config for PartyA is "test"
Password: test


Welcome to the Corda interactive shell.
You can see the available commands by typing 'help'.

# you'll see the corda shell available and can run flows
Fri May 15 18:23:03 GMT 2020>>> flow start YoFlow target: PartyB

 ✓ Starting
 ✓ Creating a new Yo!
 ✓ Signing the Yo!
 ✓ Verfiying the Yo!
 ✓ Sending the Yo!
          Requesting signature by notary service
              Requesting signature by Notary service
              Validating response from Notary service
     ✓ Broadcasting transaction to participants
▶︎ Done
Flow completed with result: SignedTransaction(id=3F92F41B699719B2CDE578959BB09F50D3D4F5D51A496DEAB67E438B2614F48C)
```

Once this runs on your machine you've got everything you would need to run corda for development using docker!

###### Note you can't send a Yo! to yourself because that's not cool!

To see all the Yo's other nodes have sent you in your vault you can run a vault query from the Corda shell:

```bash
run vaultQuery contractStateType: net.corda.examples.yo.states.YoState
```
