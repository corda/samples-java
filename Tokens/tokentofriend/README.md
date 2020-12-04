<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>
# tokentofriend
 
 ## Running the applications 
 ```
 ./gradlew deployNodes
 ./build/nodes/runnodes
 ```
 
 ## Running in terminal: 
 Go to the operator node: 
 ```
 flow start CreateMyToken myEmail: 1@gmail.com, recipients: 2@gmail.com, msg: Corda Number 1! 

 ```
 then record the returned uuid
 ```
 flow start IssueToken uuid: xxx-xxxx-xxxx-xxxx-xx
 ```
 record the message returned, TokenId and storage node.
 
 Go to that storage node terminal: 
 ```
 flow start QueryToken uuid: xxx-xxxx-xxxx-xxxx-xx, recipientEmai: 2@gmail.com
 ```
 
You should discover the message that was attached in the token. 

## Runing in webapp
Open a new window and run the blow code for token issuance
```
./gradlew runOperatoreServer
```
To retrieve the token, because most people will run the app locally, by default I have the gradle task to start only one storage node's web server. 
```
./gradlew runUSWest1Server
```
After both servers started, go to localhost:10050 to issue a token and localhost:10053 to experience the retrieve. (The reason it is two different site is that communiticating among multiple local server is prohibit by CORS policy. In production environment, we do not need to go to a different site for retrieve.)





