package net.temporus.depikture

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.text.InputType.*
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpPost
import com.google.firebase.iid.FirebaseInstanceId
import net.temporus.depikture.objects.User
import net.temporus.depikture.utils.DBHelper
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import kotlinx.android.synthetic.main.content_main.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.temporus.depikture.objects.Lobby
import net.temporus.depikture.objects.Player
import net.temporus.depikture.utils.isEmailValid
import net.temporus.depikture.utils.isNameValid
import net.temporus.depikture.utils.isPasswordValid


class MainActivity : AppCompatActivity() {

    private val logTag = "Main"
    private var dbHelper: DBHelper? = null
    private var user: User? = null
    private var isLoggedIn: Boolean = false
    private var currentDialog: DialogInterface? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        FuelManager.instance.apply {
            basePath = "https://temporus.net/api"
            baseHeaders = mapOf("Device" to "Android")
        }

        dbHelper = DBHelper(this)
        user = dbHelper!!.getUser()
        isLoggedIn = user != null

        if (isLoggedIn) {
            loginFab.hide()
            newGameBtn.visibility = View.VISIBLE
        }

        newGameBtn.setOnClickListener{
            currentDialog = alert {
                title = "Create lobby"
                customView {
                    verticalLayout {
                        padding = dip(24)
                        button("Choose a default wordlist") {
                            padding = dip(8)
                            textSize = 20f
                            onClick {
                                currentDialog!!.dismiss()
                                this@MainActivity.createWithDefaultDialog()
                            }
                        }
                        textView("or") {
                            gravity = Gravity.CENTER
                            textSize = 22f
                            textColor = Color.WHITE
                        }
                        button("Use a custom wordlist") {
                            padding = dip(8)
                            textSize = 20f
                            onClick {
                                currentDialog!!.dismiss()
                                this@MainActivity.createWithCustomDialog()
                            }
                        }
                    }
                }
            }.show()
        }

        joinBtn.setOnClickListener {
            currentDialog = alert {
                title = "Join"
                customView {
                    verticalLayout {
                        padding = dip(24)
                        val title = editText {
                            hint = "Lobby title"
                            textSize = 20f
                        }
                        val username = editText {
                            hint = "Username"
                            textSize = 20f
                        }
                        button("Join") {
                            padding = dip(8)
                            textSize = 22f
                            onClick {
                                val titleVal = title.text.toString()
                                val usernameVal = username.text.toString()
                                if (isNameValid(titleVal) && isNameValid(usernameVal)) {
                                    currentDialog!!.dismiss()
                                    val dialog = indeterminateProgressDialog(
                                            message = "Please wait a bit…",
                                            title = "Joining ${title.text} as, ${username.text}")
                                    val player = Player()
                                    player.username = usernameVal
                                    attemptJoin(titleVal, player, dialog)
                                } else {
                                    toast("Enter a valid lobby title")
                                }
                            }
                        }
                    }
                }
            }.show()
        }

        loginFab.setOnClickListener {
            currentDialog = alert {
                title = "Log in"
                customView {
                    verticalLayout {
                        padding = dip(24)
                        val email = editText {
                            hint = "Email"
                            textSize = 20f
                        }
                        val password = editText {
                            hint = "Password"
                            textSize = 20f
                            inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                        }
                        button("Login") {
                            padding = dip(8)
                            textSize = 22f
                            onClick {
                                val emailVal = email.text.toString()
                                val passwordVal = password.text.toString()
                                if (isEmailValid(emailVal) && isPasswordValid(passwordVal)) {
                                    currentDialog!!.dismiss()
                                    val dialog = indeterminateProgressDialog(
                                            message = "Please wait a bit…",
                                            title = "Logging in as ${email.text}")
                                    val user = User()
                                    user.email = emailVal
                                    user.password = passwordVal
                                    attemptAuth(user, dialog)
                                } else {
                                    toast("Enter a valid email & password")
                                }
                            }
                        }
                    }
                }
            }.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun createWithDefaultDialog() {
        currentDialog = alert {
            title = "Create lobby"
            customView {
                verticalLayout {
                    padding = dip(24)
                    val title = editText {
                        hint = "Lobby title"
                        textSize = 20f
                    }
                    button("Next") {
                        padding = dip(8)
                        textSize = 22f
                        onClick {
                            val titleVal = title.text.toString()
                            if (isNameValid(titleVal)) {
                                currentDialog!!.dismiss()
                                val lobby = Lobby()
                                lobby.title = titleVal
                                val wordlists = listOf("German", "English")
                                selector("Select a wordlist", wordlists, { _, i ->
                                    currentDialog!!.dismiss()
                                    val dialog = indeterminateProgressDialog(
                                            message = "Please wait a bit…",
                                            title = "Creating lobby")
                                    lobby.wordlist = wordlists[i]
                                    attemptCreate(lobby, user!!, dialog)
                                })
                            } else {
                                toast("Title has to be at least 4 letters")
                            }
                        }
                    }
                }
            }
        }.show()
    }

    private fun createWithCustomDialog() {
        currentDialog = alert {
            title = "Create lobby"
            customView {
                verticalLayout {
                    padding = dip(24)
                    val wordList = editText {
                        hint = "Wordlist"
                        textSize = 20f
                    }
                    val title = editText {
                        hint = "Lobby title"
                        textSize = 20f
                    }
                    button("Create") {
                        padding = dip(8)
                        textSize = 22f
                        onClick {
                            val titleVal = title.text.toString()
                            val wordlistVal = wordList.text.toString()
                            if (isNameValid(titleVal) && isNameValid(wordlistVal)) {
                                currentDialog!!.dismiss()
                                val dialog = indeterminateProgressDialog(
                                        message = "Please wait a bit…",
                                        title = "Creating lobby")
                                val lobby = Lobby()
                                lobby.title = titleVal
                                lobby.wordlist = wordlistVal
                                attemptCreate(lobby, user!!, dialog)
                            } else {
                                toast("Title has to be at least 4 letters")
                            }
                        }
                    }
                }
            }
        }.show()
    }

    private fun attemptAuth(credentials: User, progress: DialogInterface) {
        "/users/login".httpPost()
                .body("{\"user\": ${Gson().toJson(credentials)}}")
                .header("Content-Type" to "application/json")
                .responseJson { request, _, result ->
                    progress.dismiss()
                    Log.d(logTag, "Req: " + request.toString())
                    Log.d(logTag, "Res: " + result.toString())
                    result.fold(success = { json ->
                        val userObj = json.obj().getJSONObject("user")
                        this.user = User(
                                0,
                                userObj.getString("username"),
                                userObj.getString("email"),
                                credentials.password!!,
                                userObj.getString("token")
                        )
                        Log.d("User", this.user!!.username)
                        dbHelper!!.insertUser(this.user!!)
                        loginFab.hide()
                        newGameBtn.visibility = View.VISIBLE
                    }, failure = { error ->
                        Log.e("error", error.toString())
                    })
                }
    }

    private fun attemptJoin(title: String, player: Player, progress: DialogInterface) {
        if (FirebaseInstanceId.getInstance().token != null) {
            Log.d(logTag, "Token: " + FirebaseInstanceId.getInstance().token)
            player.instanceID = FirebaseInstanceId.getInstance().token
            "/lobbies/l/join/$title".httpPost()
                    .body("{\"player\": ${Gson().toJson(player)}}")
                    .header("Content-Type" to "application/json")
                    .responseJson { request, response, result ->
                        progress.dismiss()
                        Log.d(logTag, "Req: " + request.toString())
                        Log.d(logTag, "Res: " + response.toString())
                        Log.d(logTag, "Result: " + result.toString())
                        result.fold(success = { json ->
                            val lobbyMap: Map<String, Lobby> = Gson().fromJson(json.content, object : TypeToken<Map<String, Lobby>>() {}.type)
                            val lobby: Lobby = lobbyMap["lobby"]!!
                            val playerObj = json.obj().getJSONObject("player")
                            player.token = playerObj.getString("token")
                            lobby.currentPlayer = player
                            lobby.isOwner = false
                            startActivity(Intent(this, LobbyActivity::class.java)
                                    .putExtra("lobby", lobby)
                            )
                        }, failure = { error ->
                            Log.e("error", error.toString())
                        })
                    }
        } else {
            toast("Google Firebase isn't working, try again later!")
        }
    }

    private fun attemptCreate(lobbyData: Lobby, user: User, progress: DialogInterface) {
        if (FirebaseInstanceId.getInstance().token != null) {
            Log.d(logTag, "Token: " + FirebaseInstanceId.getInstance().token)
            val player = user as Player
            player.username = user.name
            player.instanceID = FirebaseInstanceId.getInstance().token
            "/lobbies".httpPost()
                    .body("{\"lobby\": ${Gson().toJson(lobbyData)}, \"instanceID\": \"${player.instanceID}\"}")
                    .header("Content-Type" to "application/json", "Authorization" to "Token " + user.jwt)
                    .responseJson { request, response, result ->
                        progress.dismiss()
                        Log.d(logTag, "Req: " + request.toString())
                        Log.d(logTag, "Res: " + response.toString())
                        result.fold(success = { json ->
                            val lobbyMap: Map<String, Lobby> = Gson().fromJson(json.content, object : TypeToken<Map<String, Lobby>>() {}.type)
                            val lobby: Lobby = lobbyMap["lobby"]!!
                            val playerObj = json.obj().getJSONObject("player")
                            player.token = playerObj.getString("token")
                            lobby.currentPlayer = player
                            lobby.isOwner = true
                            startActivity(Intent(this, LobbyActivity::class.java)
                                    .putExtra("lobby", lobby)
                                    .putExtra("jwt", user.jwt)
                            )

                        }, failure = { error ->
                            Log.e("error", error.toString())
                            toast("Couldn't create lobby")
                        })
                    }
        }
    }
}