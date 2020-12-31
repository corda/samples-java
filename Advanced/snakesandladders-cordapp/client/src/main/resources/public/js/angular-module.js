const app = angular.module('snlApp', ['ui.bootstrap', 'toastr']);

let httpHeaders = {
        headers : {
            "Content-Type": "application/x-www-form-urlencoded"
        }
    };

app.controller('AppController', function($http, toastr, $uibModal, $interval) {
    const demoApp = this;
    const apiBaseURL = "/api/snl/";

    demoApp.landingScreen = true;
    demoApp.gameScreen = false;
    demoApp.showSpinner = false;
    demoApp.pristine = true;
    demoApp.isP1 = false;
    demoApp.player = "";
    demoApp.playerNum = 1;
    demoApp.rolledFlag = false;
    demoApp.dice = {
        diceRollFlag : false,
        roll: 6,
        id: "die-6"
    }
    demoApp.game = {};

    demoApp.console = {
        welcome: "Welcome to Snakes and Ladders powered by Corda!",
        myTurn: "You turn to play. Roll the Dice",
        myRoll: "You rolled ",
//        snake: "Oops!! You landed on a snake!",
//        ladder: "Great!! You found on a ladder!",
        winner: "Congratulation!! You WON.",
        loser: " won. Hard Luck!! You Lose.",
        opTurn: "Waiting for your opposition to complete their turn.",
        opRoll: "Opposition Player rolled "
//        oppSnake: "And he landed on a snake.",
//        oppLadder: "And ge found on a ladder.",
    };

    demoApp.consoleMessages = [demoApp.console.welcome];

    demoApp.startGame = () => {
        demoApp.showSpinner = true;
        $http.post(apiBaseURL + 'createGame', {player1: demoApp.player1, player2: demoApp.player2})
        .then((response) => {
            if(response.data && response.data.status){
                toastr.success('Game Created Successfully!');
                var gameId = response.data.data;
                demoApp.gameScreen = true;
                demoApp.landingScreen = false;
                demoApp.game = demoApp.fetchGame(gameId, 1);
                demoApp.isP1 = true;
            }else{
                toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
            }
            demoApp.showSpinner = false;
        });
    }

    demoApp.loadGame = (gameId, player) => {
        if(player ==1){
            demoApp.isP1 = true;
        }
        demoApp.fetchGame(gameId, player, 0);
    }

    demoApp.rollDice = () => {
        demoApp.showSpinner = true;
            $http.get(apiBaseURL + 'rollDice')
            .then((response) => {
                if(response.data && response.data.status){
                    demoApp.dice.diceRollFlag = !demoApp.dice.diceRollFlag;
                    var roll = response.data.data
                    demoApp.dice.roll = roll;
                    demoApp.dice.id = "die-" + roll;
                    demoApp.playMove(roll);
                }else{
                    toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
                }
                demoApp.showSpinner = false;
        });
    }


    demoApp.playMove = (rolledNumber) => {
        demoApp.showSpinner = true;
        $http.post(apiBaseURL + 'playerMove', {player: demoApp.player, gameId: demoApp.game.linearId.id, rolledNumber: rolledNumber})
        .then((response) => {
            if(response.data && response.data.status){
                demoApp.rolledFlag = true;
                demoApp.pristine = false;
                demoApp.fetchGame(demoApp.game.linearId.id);
            }else{
                toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
            }
            demoApp.showSpinner = false;
        });
    }

    // 0 - loadGame
    demoApp.animatePlayer = (flag) => {
        var playerSelector = "";
        if(flag == 0){
            updateP1Pos();
            updateP2Pos();
        }else{
            if(demoApp.playerNum ==1){
                updateP1Pos();
            }else if(demoApp.playerNum == 2){
                 updateP2Pos();
            }
        }
    }

    const updateP1Pos = () => {
        // X position
        if((demoApp.game.player1Pos % 20 == 0))
            angular.element(document.querySelector('#player1'))[0].style.left = (((demoApp.game.player1Pos-1) %10)*80) + 10 - 720+ "px";
        else if((parseInt(demoApp.game.player1Pos/10) % 2 == 0) || (demoApp.game.player1Pos % 10 == 0 ))
            angular.element(document.querySelector('#player1'))[0].style.left = (((demoApp.game.player1Pos-1) %10)*80) + 10 + "px"
        else
            angular.element(document.querySelector('#player1'))[0].style.left = ((10 - (demoApp.game.player1Pos %10))*80) + 10 + "px"

        // Y Position

        if(parseInt(demoApp.game.player1Pos/10) % 10 == 0 && parseInt(demoApp.game.player1Pos/10) != 0 || demoApp.game.player1Pos%10 == 0)
            angular.element(document.querySelector('#player1'))[0].style.top = 750 - (parseInt(demoApp.game.player1Pos/10) * 80) + 80 + "px";
        else
            angular.element(document.querySelector('#player1'))[0].style.top = 750 - (parseInt(demoApp.game.player1Pos/10) * 80) + "px";
    }

    const updateP2Pos = () => {
        if((demoApp.game.player2Pos % 20 == 0))
            angular.element(document.querySelector('#player2'))[0].style.left = (((demoApp.game.player2Pos-1) %10)*80) + 30 - 720+ "px";
        else if((parseInt(demoApp.game.player2Pos/10) % 2 == 0) || (demoApp.game.player2Pos % 10 == 0 ))
            angular.element(document.querySelector('#player2'))[0].style.left = (((demoApp.game.player2Pos-1) %10)*80) + 30 + "px";
        else
            angular.element(document.querySelector('#player2'))[0].style.left = ((10 - (demoApp.game.player2Pos %10))*80) + 30 + "px"

        if(parseInt(demoApp.game.player2Pos/10) % 10 == 0 && parseInt(demoApp.game.player2Pos/10) != 0 || demoApp.game.player2Pos%10 == 0)
            angular.element(document.querySelector('#player2'))[0].style.top = 750 - (parseInt(demoApp.game.player2Pos/10) * 80) + 80 + "px";
        else
            angular.element(document.querySelector('#player2'))[0].style.top = 750 - (parseInt(demoApp.game.player2Pos/10) * 80) + "px";
    }


    demoApp.openCreateAccountModal = () => {
        const accountModel = $uibModal.open({
            templateUrl: 'createAccountModal.html',
            controller: 'CreateAccountModalCtrl',
            controllerAs: 'createAccountModalCtrl',
            windowClass: 'app-modal-window',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                toastr: () => toastr,
            }
        });

        accountModel.result.then(() => {}, () => {});
    };

    demoApp.fetchGame = (gameId, player, flag) => {
       $http.get(apiBaseURL + 'getGame/' + gameId)
       .then((response) => {
          if(response.data && response.data.status){
              demoApp.gameScreen = true;
              demoApp.landingScreen = false;
              demoApp.game = response.data.data;
              if(player == 1){
                    demoApp.player = demoApp.game.player1;
                    demoApp.playerNum  = 1;
                }else if(player == 2){
                    demoApp.player = demoApp.game.player2;
                    demoApp.playerNum  = 2;
                }
                demoApp.animatePlayer(flag);
                if(demoApp.game.winner != null){
                    demoApp.handleConsole(true);
                }else{
                    demoApp.handleConsole();
                }

          }else{
              toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
          }
       });
    }

    demoApp.handleConsole = (gameOver) => {
        demoApp.consoleMessages = [];
        if(gameOver){
            if(demoApp.game.winner == demoApp.player){
                demoApp.consoleMessages.push(demoApp.console.winner);
            }else{
                demoApp.consoleMessages.push(demoApp.game.winner + " " + demoApp.console.loser);
            }
        }else{
            demoApp.consoleMessages.push(demoApp.console.welcome);
            if(demoApp.rolledFlag){
                demoApp.consoleMessages.push(demoApp.console.myRoll + " " + demoApp.game.lastRoll +
                ". Your new position is " + (demoApp.isP1? demoApp.game.player1Pos:demoApp.game.player2Pos));
            }

            if(demoApp.player == demoApp.game.currentPlayer){
                if(!demoApp.pristine){
                    demoApp.consoleMessages.push(demoApp.console.opRoll + " " + demoApp.game.lastRoll +
                        ". Their new position is " + (demoApp.isP1? demoApp.game.player2Pos:demoApp.game.player1Pos));
                }
                demoApp.consoleMessages.push(demoApp.console.myTurn);
            }
            else{
                demoApp.pristine = false;
                demoApp.consoleMessages.push(demoApp.console.opTurn);
            }
        }
    }

    $interval(function(){
        console.log("Executed");
        if(demoApp.game.linearId != undefined)
            demoApp.fetchGame(demoApp.game.linearId.id, undefined, 0);
    },5000)

});


app.controller('CreateAccountModalCtrl', function ($http, $uibModalInstance, $uibModal, demoApp, apiBaseURL, toastr) {
    const createAccountModel = this;

    createAccountModel.form = {};

    createAccountModel.create = () => {
        if(createAccountModel.form.name == undefined || createAccountModel.form.name == '' ){
           toastr.error("Enter Gamer Account Name!");
        }else{
           demoApp.showSpinner = true;
           $http.get(apiBaseURL + 'account/create/' + createAccountModel.form.name)
           .then((response) => {
              if(response.data && response.data.status){
                  toastr.success('Gamer Account Create Successfully');
                  $uibModalInstance.dismiss();
              }else{
                  toastr.error(response.data? response.data.message: "Something went wrong. Please try again later!");
              }
              demoApp.showSpinner = false;
           });
        }
    }

    createAccountModel.cancel = () => $uibModalInstance.dismiss();

});
