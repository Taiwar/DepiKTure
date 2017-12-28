package net.temporus.depikture.objects

import java.io.Serializable

class Lobby : Serializable {
    var title: String? = null
    var owner: String? = null
    var players: List<Player>? = null
    var descriptions: List<Description>? = null
    var currentPlayer: Player? = null
    var status: String? = null
    var wordlist: String? = null
    var isOwner: Boolean = false

    constructor() {
        this.title = ""
        this.owner = ""
        this.players = null
        this.currentPlayer = null
        this.status = ""
    }

    internal constructor(title: String, owner: String, currentPlayer: Player,
                         status: String, isOwner: Boolean) {
        this.title = title
        this.owner = owner
        this.currentPlayer = currentPlayer
        this.status = status
        this.isOwner = isOwner
    }
}
