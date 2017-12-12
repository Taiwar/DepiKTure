package net.temporus.depikture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPut
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.content_lobby.*

import net.temporus.depikture.objects.Lobby
import net.temporus.depikture.objects.Player
import org.jetbrains.anko.*

class LobbyActivity : AppCompatActivity() {

    private val logTag = "Main"
    private var lobby = Lobby()
    private var lobbyContent: ConstraintLayout? = null
    private var mBroadcastReceiver: BroadcastReceiver? = null
    private var player: Player? = null
    private var isOwner: Boolean = false
    private var jwt: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lobby)

        lobbyContent = findViewById(R.id.lobby_content_layout)

        lobby = intent.getSerializableExtra("lobby") as Lobby
        isOwner = lobby.isOwner
        player = lobby.currentPlayer

        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // TODO: Implement function to confirm reception of message

                val header = intent.getStringExtra("header")
                val extra: String = intent.getStringExtra("extra")

                if (header === "Lobby") {
                    lobby = intent.getSerializableExtra("lobby") as Lobby
                    lobby.currentPlayer = player
                    lobby.isOwner = isOwner
                    Log.d(logTag, "Updating Lobby: $header; $lobby; $extra")
                    updateLobby(lobby, extra)
                } else if (header === "instanceID") {
                    Log.d(logTag, "Updating instanceID: $extra")
                    updateInstanceID(extra)
                }

            }
        }

        if (isOwner) {
            jwt = intent.getStringExtra("jwt")
            messageView.visibility = View.GONE
            start_button.visibility = View.VISIBLE
            start_button.setOnClickListener {
                start_button.visibility = View.GONE
                val progress = indeterminateProgressDialog(
                        message = getString(R.string.wait_progress),
                        title = getString(R.string.wait_starting))
                "/lobbies/start".httpGet()
                        .header("Content-Type" to "application/json", "Authorization" to "Token " + jwt)
                        .responseJson { request, _, result ->
                            progress.dismiss()
                            Log.d(logTag, "Req: " + request.toString())
                            Log.d(logTag, "Res: " + result.toString())
                            result.fold(success = { json ->
                                val lobbyMap: Map<String, Lobby> = Gson().fromJson(json.content, object : TypeToken<Map<String, Lobby>>() {}.type)
                                val lobby: Lobby = lobbyMap["lobby"]!!
                                lobby.currentPlayer = player
                                lobby.isOwner = isOwner
                                Log.d("Started lobby", lobby.title)
                            }, failure = { error ->
                                start_button.visibility = View.VISIBLE
                                toast(error.toString())
                            })
                        }
            }

            next_button.setOnClickListener {
                val progress = indeterminateProgressDialog(
                        message = getString(R.string.wait_progress),
                        title = getString(R.string.wait_starting_round))
                "/lobbies/next".httpGet()
                        .header("Content-Type" to "application/json", "Authorization" to "Token " + jwt)
                        .responseJson { request, _, result ->
                            next_button.visibility = View.GONE
                            progress.dismiss()
                            Log.d(logTag, "Req: " + request.toString())
                            Log.d(logTag, "Res: " + result.toString())
                            result.fold(success = { json ->
                                val lobbyMap: Map<String, Lobby> = Gson().fromJson(json.content, object : TypeToken<Map<String, Lobby>>() {}.type)
                                val lobby: Lobby = lobbyMap["lobby"]!!
                                lobby.currentPlayer = player
                                lobby.isOwner = isOwner
                            }, failure = { error ->
                                toast(error.toString())
                            })
                        }
            }
        }

        titleView.text = lobby.title
        messageView.text = resources.getString(R.string.message_default)
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
        super.onDestroy()
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
        super.onPause()
    }

    override fun onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, IntentFilter("DFIS"))
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, IntentFilter("DFMS"))
        super.onResume()
    }

    override fun onBackPressed() {
        if (isOwner) {
            alert {
                title = getString(R.string.q_stop_game)
                yesButton {
                    val progress = indeterminateProgressDialog(
                            message = getString(R.string.wait_progress),
                            title = getString(R.string.wait_stopping)
                    )
                    "/lobbies/stop".httpGet()
                            .header("Content-Type" to "application/json", "Authorization" to "Token " + jwt)
                            .responseJson { request, _, result ->
                                progress.dismiss()
                                Log.d(logTag, "Req: " + request.toString())
                                Log.d(logTag, "Res: " + result.toString())
                                result.fold(success = { json ->
                                    val lobbyMap: Map<String, Lobby> = Gson().fromJson(json.content, object : TypeToken<Map<String, Lobby>>() {}.type)
                                    val lobby: Lobby = lobbyMap["lobby"]!!
                                    lobby.currentPlayer = player
                                    lobby.isOwner = isOwner
                                    finish()
                                }, failure = { error ->
                                    toast(error.toString())
                                })
                            }
                }
                noButton {}
            }.show()
        }
    }

    private fun updateInstanceID(instanceID: String) {
        this.player!!.instanceID = instanceID
        "/players".httpPut()
                .body("{\"player\": ${Gson().toJson(player)}}")
                .header("Content-Type" to "application/json")
                .responseJson { request, response, result ->
                    Log.d(logTag, "Req: " + request.toString())
                    Log.d(logTag, "Res: " + response.toString())
                    Log.d(logTag, "Result: " + result.toString())
                    result.fold(success = { json ->
                        val playerObj = json.obj().getJSONObject("player")
                        this.player = Player(
                                playerObj.getString("username"),
                                playerObj.getString("token"),
                                playerObj.getString("instanceID")
                        )
                        Log.d("player", this.player!!.username)
                    }, failure = { error ->
                        toast(error.toString())
                    })
                }
    }

    private fun updateLobby(lobby: Lobby, extra: String) {
        Log.d(logTag, "Status: " + lobby.status)
        when (lobby.status) {
            "ping" -> {
                // TODO: Send response to server
            }
            "resubmit" -> {
                longToast(R.string.correct_description)
                val intent = Intent(this, SubmitDescriptionActivity::class.java)
                intent.putExtra("lobby", lobby)
                startActivity(intent)
            }
            "started" -> {
                Log.d(logTag, "Lobby started!")
                val intent = Intent(this, DrawActivity::class.java)
                intent.putExtra("lobby", lobby)
                intent.putExtra("word", extra)
                start_button.visibility = View.GONE
                messageView.visibility = View.VISIBLE
                messageView.setText(R.string.wait_message)
                startActivity(intent)
            }
            "stage 1" -> {
                val intent = Intent(this, SubmitDescriptionActivity::class.java)
                intent.putExtra("lobby", lobby)
                startActivity(intent)
            }
            "stage 2" -> {
                val intent = Intent(this, SelectDescriptionActivity::class.java)
                intent.putExtra("lobby", lobby)
                intent.putExtra("colors", extra)
                startActivity(intent)
            }
            "stage 3" -> {
                toast(R.string.round_ended)
                if (isOwner) {
                    messageView.visibility = View.GONE
                    next_button.visibility = View.VISIBLE
                }
            }
            "ended" -> {
                toast(R.string.game_ended)
                this.finish()
            }
        }
    }

}
