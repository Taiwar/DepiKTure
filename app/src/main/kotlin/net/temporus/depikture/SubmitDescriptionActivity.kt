package net.temporus.depikture

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpPut
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_submit_description.*

import net.temporus.depikture.objects.Lobby
import net.temporus.depikture.objects.Player
import org.jetbrains.anko.alert
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton

class SubmitDescriptionActivity : AppCompatActivity() {

    private val tag = "SuDA"
    private var lobby: Lobby? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit_description)

        lobby = intent.getSerializableExtra("lobby") as Lobby

        submit_button.setOnClickListener {
            alert {
                title = "Submit this description?"
                yesButton {
                    val progress = indeterminateProgressDialog(
                            message = "Please wait a bit…",
                            title = "Loading")
                    lobby!!.currentPlayer!!.description = description_field.text.toString()
                    uploadDescription(lobby!!.currentPlayer!!, progress)
                    this@SubmitDescriptionActivity.finish()
                }
                noButton {}
            }.show()
        }
    }

    override fun onBackPressed() {}

    private fun uploadDescription(player: Player, progress: DialogInterface) {
        "/players".httpPut()
                .body("{\"player\": ${Gson().toJson(player)} , \"stage\": 1}")
                .header("Content-Type" to "application/json")
                .responseJson { request, _, result ->
                    progress.dismiss()
                    Log.d(tag, "Req: " + request.toString())
                    Log.d(tag, "Res: " + result.toString())
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
