package net.temporus.depikture

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpPut
import com.google.gson.Gson

import net.temporus.depikture.objects.Lobby
import net.temporus.depikture.objects.Player
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

class SelectDescriptionActivity : AppCompatActivity() {

    private val logTag = "SeDA"
    var lobby: Lobby? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lobby = intent.getSerializableExtra("lobby") as Lobby
        verticalLayout {
            padding = dip(30)
            backgroundColor = resources.getColor(R.color.colorPrimary, null)
            lobby!!.descriptions!!.forEach { description ->
                button(description.description) {
                    backgroundColor = Color.parseColor(description.color)
                    textSize = 26f
                    textColor = resources.getColor(R.color.textColorPrimary, null)
                    onClick {
                        alert {
                            title = "Choose this description?"
                            yesButton {
                                val progress = indeterminateProgressDialog(
                                        message = "Please wait a bit…",
                                        title = "Loading")
                                lobby!!.currentPlayer!!.selection = description.color
                                uploadSelection(lobby!!.currentPlayer!!, progress)
                                this@SelectDescriptionActivity.finish()
                            }
                            noButton {}
                        }.show()
                    }
                }.lparams(width = dip(300)) {
                    verticalMargin = dip(5)
                }
            }
        }
    }

    override fun onBackPressed() {}

    private fun uploadSelection(player: Player, progress: DialogInterface) {
        "/players".httpPut()
                .body("{\"player\": ${Gson().toJson(player)} , \"stage\": 2}")
                .header("Content-Type" to "application/json")
                .responseJson { request, _, result ->
                    progress.dismiss()
                    Log.d(logTag, "Req: " + request.toString())
                    Log.d(logTag, "Res: " + result.toString())
                    result.fold(success = { json ->
                        val playerObj = json.obj().getJSONObject("player")
                        val newPlayer = Player(
                                playerObj.getString("username"),
                                playerObj.getString("token"),
                                playerObj.getString("instanceID")
                        )
                        Log.d("Success: newPlayer", newPlayer.username)
                        this.finish()
                    }, failure = { error ->
                        Log.e("error", error.toString())
                    })
                }
    }
}
