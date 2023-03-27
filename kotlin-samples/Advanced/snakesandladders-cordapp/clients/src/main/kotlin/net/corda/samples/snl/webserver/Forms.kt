package net.corda.samples.snl.webserver

class Forms {
    class CreateGameForm {
        var player1: String? = null
        var player2: String? = null
    }

    class PlayerMoveForm {
        var player: String? = null
        val gameId: String? = null
        val rolledNumber = 0
    }
}
