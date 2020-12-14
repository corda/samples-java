package net.corda.samples.client;

public class Forms {

    public static class CreateGameForm {
        private String player1;
        private String player2;

        public String getPlayer1() {
            return player1;
        }

        public void setPlayer1(String player1) {
            this.player1 = player1;
        }

        public String getPlayer2() {
            return player2;
        }

        public void setPlayer2(String player2) {
            this.player2 = player2;
        }
    }

    public static class PlayerMoveForm {
        private String player;
        private String gameId;
        private int rolledNumber;

        public String getPlayer() {
            return player;
        }

        public void setPlayer(String player) {
            this.player = player;
        }

        public String getGameId() {
            return gameId;
        }

        public int getRolledNumber() {
            return rolledNumber;
        }
    }

}
