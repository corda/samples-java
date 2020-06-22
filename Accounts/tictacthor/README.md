# Tic Tac Thor [<img src="../../webIDE.png" height=25 />](https://ide.corda.net/?folder=/home/coder/samples-java/Accounts/tictacthor)
This CorDapp recreates the game of Tic Tac Toe via Corda. It primarilly demonstrates how you can have [LinearState](https://docs.corda.net/docs/corda-os/api-states.html#linearstate) transactions between cross-node accounts.

<p align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/3/32/Tic_tac_toe.svg/1024px-Tic_tac_toe.svg.png" alt="Corda" width="200">
</p>

## Running the sample
Deploy and run the nodes by:
```
./gradlew deployNodes
./build/nodes/runnodes
```
Then you will need to also start the spring server for the Cordapp by running the following commands seperately: 
`./gradlew bootRunDevRel`will have the DevRel server running on 8080 port 
, and `./gradlew bootRunSOE`will start the Solution Engineering server on 8090 port

## Operating the Cordapp
Now go to postman and excute the following in order: (All of the API are POST request!)
1. Create an account on DevRel node: `http://localhost:8080/createAccount/PeterLi` 
2. Create an account on SoE node: `http://localhost:8090/createAccount/DavidWinner`
3. Peter now requests game with David: `http://localhost:8080/requestGameWith/PeterLi/SolutionEng/DavidWinner` 
4. David has to accept the challenege: `http://localhost:8090/acceptGameInvite/DavidWinner/DevRel/PeterLi`
5. Game Starts, and Peter makes the first move: `http://localhost:8080/startGameAndFirstMove/PeterLi/DavidWinner/0`
6. David's turn: `http://localhost:8090/submitMove/DavidWinner/PeterLi/4`
API Syntax: `http://localhost:8080/submitMove/WHO-AM-I/MY-COUNTERPART/POSITION` 

From here, you can start play the game by changing the very last number from the `submitMove`API call. The game board is representated by an 1-D array: What we just ran can transfer into a tic-tac-toe game board like the one we see on the right.
```
│0│1│2│                  │O│ │ │
│3│4│5│        ->        │ │X│ │
│6│7│8│                  │ │ │ │
```
The Game will automatically end when one player wins the game. 
You can also run `run vaultQuery contractStateType: net.corda.samples.tictacthor.states.BoardState` at any given time to see the board games stored in vault. 

now if you want to fast forward the game, Play the following moves in order:
According to syntax: we should have `http://localhost:8080/submitMove/PeterLi/DavidWinner/3` for the first move below.
```
* Peter: 3                                  │O│ │ │
* David: 5     This will yield a game like: │O│X│X│
* Peter: 6                                  │O│ │ │
```
And Peter won! 

## Highlights about Accounts
We can play a bit more about the accounts. Now let's create two accounts, a new one for each node:
* Create an account on DevRel node: `http://localhost:8080/createAccount/AnthonyNix` 
* Create an account on SoE node: `http://localhost:8090/createAccount/ThorG`
Now, try to have Anthony play a game with Thor while start a new game between Peter and David. It worked! 

One key feature about account is that, each account's data is segregated, meaning that it can be enforced that each account will not be able to see other account's data. In this sample cordapp, the game is queried by account name. Therefore, we see that each account only knows about the game that he participated. Account Peter doesn't know anything about the game between Thor and Anthony. 

## Credit 
This project is inspired and evolved from a simple [tic-tac-toe](https://github.com/thorgilman/tictactoe) game on Corda by Thor Gilman. 








