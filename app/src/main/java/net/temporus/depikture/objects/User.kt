package net.temporus.depikture.objects

class User : Player {

    var id: Int = 0
    var name: String? = null
    var email: String? = null
    var password: String? = null
    var jwt: String? = null

    constructor() {
        this.id = 0
        this.name = ""
        this.email = ""
        this.password = ""
        this.jwt = ""
    }

    internal constructor(uid: Int, username: String, email: String, password: String, jwt: String) {
        this.id = uid
        this.name = username
        this.email = email
        this.password = password
        this.jwt = jwt
    }
}
