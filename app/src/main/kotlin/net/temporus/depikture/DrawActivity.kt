package net.temporus.depikture

import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_draw.*
import kotlinx.android.synthetic.main.content_draw.*
import kotlinx.android.synthetic.main.content_draw_waittimer.*

import net.temporus.depikture.objects.Lobby
import net.temporus.depikture.objects.Player
import net.temporus.depikture.views.DrawView

import me.priyesh.chroma.ChromaDialog
import me.priyesh.chroma.ColorMode
import me.priyesh.chroma.ColorSelectListener
import org.jetbrains.anko.alert
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton

class DrawActivity : AppCompatActivity() {

    private var drawView: DrawView? = null
    private var currPlayer: Player? = null
    private var lobby: Lobby? = null
    private val drawActivity = this
    private var mColor: Int = 0
    private var currentBrushWidth = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draw)

        lobby = intent.getSerializableExtra("lobby") as Lobby
        val word = intent.getStringExtra("word")

        currPlayer = lobby!!.currentPlayer

        drawView = DrawView(this)
        drawView!!.setBackgroundColor(Color.WHITE)
        content_draw.addView(drawView)

        mColor = savedInstanceState?.getInt(KEY_COLOR) ?: ContextCompat.getColor(this, R.color.colorPrimary)

        fab_upload.setOnClickListener {
            alert {
                title = getString(R.string.q_upload_drawing)
                yesButton {
                    Log.d("Dev", "Save")
                    val progress = indeterminateProgressDialog(
                            message = getString(R.string.wait_progress),
                            title = getString(R.string.uploading)
                    )
                    try {
                        drawView!!.saveCanvas(applicationContext, currPlayer!!)
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.d("Dev", "Couldn't save Canvas")
                        Log.d("Dev", e.toString(), e)
                    }
                    progress.dismiss()
                    drawActivity.finish()
                }
                noButton {}
            }.show()
        }

        fab_color_picker.setOnClickListener { showColorPickerDialog() }
        fab_width_picker.setOnClickListener { showWidthDialog() }
        fab_undo.setOnClickListener { drawView!!.onClickUndo() }
        fab_redo.setOnClickListener { drawView!!.onClickRedo() }

        word_to_draw.text = word
        small_word_to_draw.text = word

        object : CountDownTimer(6000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                timer_digit.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                timer_digit.text = "0"
                content_draw_waittimer.visibility = View.GONE
                fab_container.visibility = View.VISIBLE
                content_draw.visibility = View.VISIBLE
            }

        }.start()
    }

    override fun onBackPressed() {}

    private fun showColorPickerDialog() {
        ChromaDialog.Builder()
                .initialColor(mColor)
                .colorMode(ColorMode.RGB)
                .onColorSelected(object : ColorSelectListener {
                    override fun onColorSelected(color: Int) {
                        drawView!!.changeColor(color)
                        mColor = color
                    }
                })
                .create()
                .show(supportFragmentManager, "dialog")
    }

    private fun showWidthDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.stroke_width_dialog)
        dialog.setCancelable(false)
        val widthCounter = dialog.findViewById<View>(R.id.widthCounter) as TextView
        widthCounter.text = currentBrushWidth.toString()
        val set = dialog.findViewById<View>(R.id.set_button) as Button
        val exit = dialog.findViewById<View>(R.id.closeDialog) as ImageButton

        val widthBar = dialog.findViewById<View>(R.id.width_seek_bar) as SeekBar
        widthBar.max = 100
        widthBar.progress = currentBrushWidth
        widthBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                widthCounter.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        set.setOnClickListener {
            val width = widthBar.progress
            currentBrushWidth = width

            dialog.dismiss()
            drawView!!.changeWidth(width.toFloat())
        }

        exit.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    companion object {
        private val KEY_COLOR = "extra_color"
    }

}
