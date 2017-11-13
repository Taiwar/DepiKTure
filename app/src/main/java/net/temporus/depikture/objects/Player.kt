package net.temporus.depikture.objects


import java.io.Serializable

open class Player : Serializable {

    var username: String? = ""
    var selection: String? = ""
    var description: String? = ""
    var word: String? = ""
    var drawing: String? = ""
    var token: String? = null
    var instanceID: String? = null


    constructor() {
        this.username = ""
        this.token = ""
        this.instanceID = ""
    }

    internal constructor(username: String, token: String, instanceID: String) {
        this.username = username
        this.token = token
        this.instanceID = instanceID
    }
}
