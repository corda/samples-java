![](./clients/src/main/webapp/src/Components/img/secret_corda.png)

# Corda Secret Santa

This is an implementation of Secret Santa using Corda as a tool to store multiple game states.

It has a material-ui frontend that lets users create and self-service their own secret santa games. The frontend is implemented in ReactJS and the backend is implemented with a Spring Boot server and some corda flows.

You can create a game using the web frontend (or just calling the api directly with Postman), and once the game is stored, players can look up their assignments using their game id, and the app also supports an optional sendgrid integration so that you can have emails sent to the players as well.

> One tip if you're using intellij is to open the project from the intellij dialog, don't import the project directly.

## Usage

There's essentially five processes you'll need to be aware of.

- Three Corda nodes, a notary, santa, and an elf
- The backend webserver that runs the REST endpoints for the corda nodes
- The frontend webserver, a React app that sends requests to the backend.


#### Pre-Requisites

[Set up for CorDapp development](https://docs.r3.com/en/platform/corda/4.9/community/getting-set-up.html)


### Running these services

#### The three Corda nodes
To run the corda nodes you just need to run the `deployNodes` gradle task and the nodes will be available for you to run directly.

```
./gradlew clea build deployNodes
./build/nodes/runnodes
```

#### The backend webserver

Run the `runSantaServer` Gradle task. By default, it connects to the node with RPC address `localhost:10006` with
the username `user1` and the password `test`, and serves the webserver on port `localhost:10056`.

```
./gradlew runSantaServer
```

The frontend will be visible on [localhost:10056](http://localhost:10056)

##### Background Information

`clients/src/main/java/com/secretsanta/webserver/` defines a simple Spring webserver that connects to a node via RPC and allows you to interact with the node over HTTP.

The API endpoints are defined in `clients/src/main/java/com/secretsanta/webserver/Controller.java`


#### The frontend webserver

The React server can be started by going to `clients/src/main/webapp`, running `npm install` and then `npm start`.

```
cd clients/src/main/webapp
npm install
npm start
```

(Note: You might have to use node v16 and run *npm install --legacy-peer-deps* in order to get this running)

The frontend will be visible on [localhost:3000](http://localhost:3000)

#### Configuring Email with SendGrid

If you'd like to start sending email you'll need to make an account on [sendgrid.com](http://sendgrid.com) and configure a verified sender identity.

Once you've done that, create an API key and place it into `Controller.java`(the webserver for the corda nodes). After which point you can set the `sendEmail` param to `true` in your requests. In order to configure the frontend to send emails, just open `CONSTANTS.js` and set the `SEND_EMAIL` param to `true` instead of `false`.


### Testing Utilities


#### Using Postman for backend testing

I've included some simple postman tests to run against the santa server that will be helpful to you if you plan on using this. You'll find them in the `postman` folder.


#### Running tests inside IntelliJ

There are unit tests for the corda state, contract, and tests for both flows used here. You'll find them inside the various test folders.

