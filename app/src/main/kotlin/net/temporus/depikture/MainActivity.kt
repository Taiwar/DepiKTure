package net.temporus.depikture

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
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
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import kotlinx.android.synthetic.main.content_main.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.temporus.depikture.objects.Lobby
import net.temporus.depikture.objects.Player
import net.temporus.depikture.utils.*


class MainActivity : AppCompatActivity() {

    private val logTag = "Main"
    private var dbHelper: DBHelper? = null
    private var user: User? = null
    private var isLoggedIn: Boolean = false
    private var currentDialog: DialogInterface? = null
    private var prefs: SharedPreferences? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        prefs = Prefs.with(this)

        FuelManager.instance.apply {
            basePath = prefs!!.getStringAsString(applicationContext, R.string.pref_server_url,
                    R.string.pref_server_url_default)
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
                title = getString(R.string.title_create)
                customView {
                    verticalLayout {
                        padding = dip(24)
                        button(getString(R.string.choose_default_word_list)) {
                            padding = dip(8)
                            textSize = 20f
                            onClick {
                                currentDialog!!.dismiss()
                                this@MainActivity.createWithDefaultDialog()
                            }
                        }
                        textView(getString(R.string.or)) {
                            gravity = Gravity.CENTER
                            textSize = 22f
                            textColor = Color.WHITE
                        }
                        button(getString(R.string.enter_custom_word_list_id)) {
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
                title = getString(R.string.title_join)
                customView {
                    verticalLayout {
                        padding = dip(24)
                        val title = editText {
                            hint = getString(R.string.hint_lobby_title)
                            textSize = 20f
                        }
                        val username = editText {
                            hint = getString(R.string.hint_username)
                            textSize = 20f
                        }
                        button(getString(R.string.btn_join)) {
                            padding = dip(8)
                            textSize = 22f
                            onClick {
                                val titleVal = title.text.toString()
                                val usernameVal = username.text.toString()
                                if (isNameValid(titleVal) && isNameValid(usernameVal)) {
                                    currentDialog!!.dismiss()
                                    val progress = indeterminateProgressDialog(
                                            message = getString(R.string.wait_progress),
                                            title = getString(R.string.wait_joining)
                                    )
                                    val player = Player()
                                    player.username = usernameVal
                                    attemptJoin(titleVal, player, progress)
                                } else {
                                    toast(getString(R.string.prompt_lobby_title))
                                }
                            }
                        }
                    }
                }
            }.show()
        }

        loginFab.setOnClickListener {
            currentDialog = alert {
                title = getString(R.string.title_login)
                customView {
                    verticalLayout {
                        padding = dip(24)
                        val email = editText {
                            hint = getString(R.string.hint_email)
                            textSize = 20f
                        }
                        val password = editText {
                            hint = getString(R.string.hint_password)
                            textSize = 20f
                            inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                        }
                        button(getString(R.string.title_login)) {
                            padding = dip(8)
                            textSize = 22f
                            onClick {
                                val emailVal = email.text.toString()
                                val passwordVal = password.text.toString()
                                if (isEmailValid(emailVal) && isPasswordValid(passwordVal)) {
                                    currentDialog!!.dismiss()
                                    val progress = indeterminateProgressDialog(
                                            message = getString(R.string.wait_progress),
                                            title = getString(R.string.wait_login)
                                    )
                                    val user = User()
                                    user.email = emailVal
                                    user.password = passwordVal
                                    attemptAuth(user, progress)
                                } else {
                                    toast(getString(R.string.error_invalid))
                                }
                            }
                        }
                    }
                }
            }.show()
        }
    }

    private fun createWithDefaultDialog() {
        currentDialog = alert {
            title = getString(R.string.title_create)
            customView {
                verticalLayout {
                    padding = dip(24)
                    val title = editText {
                        hint = getString(R.string.hint_wordlist_title)
                        textSize = 20f
                    }
                    button(getString(R.string.next)) {
                        padding = dip(8)
                        textSize = 22f
                        onClick {
                            val titleVal = title.text.toString()
                            if (isNameValid(titleVal)) {
                                currentDialog!!.dismiss()
                                val lobby = Lobby()
                                lobby.title = titleVal
                                val wordlists = listOf("German", "English")
                                selector(getString(R.string.select_wordlist), wordlists, { _, i ->
                                    currentDialog!!.dismiss()
                                    val progress = indeterminateProgressDialog(
                                            message = getString(R.string.wait_progress),
                                            title = getString(R.string.wait_creating)
                                    )
                                    lobby.wordlist = wordlists[i]
                                    attemptCreate(lobby, user!!, progress)
                                })
                            } else {
                                toast(getString(R.string.prompt_lobby_title_invalid))
                            }
                        }
                    }
                }
            }
        }.show()
    }

    private fun createWithCustomDialog() {
        currentDialog = alert {
            title = getString(R.string.title_create)
            customView {
                verticalLayout {
                    padding = dip(24)
                    val wordList = editText {
                        hint = getString(R.string.hint_wordlist_title)
                        textSize = 20f
                    }
                    val title = editText {
                        hint = getString(R.string.hint_lobby_title)
                        textSize = 20f
                    }
                    button(getString(R.string.create)) {
                        padding = dip(8)
                        textSize = 22f
                        onClick {
                            val titleVal = title.text.toString()
                            val wordlistVal = wordList.text.toString()
                            if (isNameValid(titleVal) && isNameValid(wordlistVal)) {
                                currentDialog!!.dismiss()
                                val dialog = indeterminateProgressDialog(
                                        message = getString(R.string.wait_progress),
                                        title = getString(R.string.wait_creating)
                                )
                                val lobby = Lobby()
                                lobby.title = titleVal
                                lobby.wordlist = wordlistVal
                                attemptCreate(lobby, user!!, dialog)
                            } else {
                                toast(getString(R.string.prompt_lobby_title_invalid))
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
                            toast(getString(R.string.error_join))
                        })
                    }
        } else {
            toast(getString(R.string.error_firebase))
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
                            toast(getString(R.string.error_create))
                        })
                    }
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
        } else if(id == R.id.action_about) {
            startActivity(Intent(this@MainActivity, AboutActivity::class.java))
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}