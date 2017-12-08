package net.temporus.depikture.views

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.DataPart
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.fuel.httpUpload
import com.google.gson.Gson
import net.temporus.depikture.R

import net.temporus.depikture.objects.Player

import java.io.File
import java.io.FileOutputStream
import java.util.HashMap
import java.util.LinkedList


class DrawView : View, OnTouchListener {


    private var mCanvas: Canvas? = null
    private var mPath: Path? = null
    private var mPaint: Paint? = null
    private val paths = LinkedList<Path>()
    private val undonePaths = LinkedList<Path>()
    private val colorsMap = HashMap<Path, Int>()
    private val widthMap = HashMap<Path, Float>()
    private var bitmap: Bitmap? = null
    private var selectedColor = Color.BLACK
    private var selectedWidth = 10f
    private var mX: Float = 0.toFloat()
    private var mY: Float = 0.toFloat()
    private val TAG = "Main"

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context) : super(context) {
        isFocusable = true
        isFocusableInTouchMode = true

        this.setOnTouchListener(this)

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val height = size.y
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.isDither = true
        paint.color = selectedColor
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = selectedWidth
        mPaint = paint
        mCanvas = Canvas(bitmap)
        mPath = Path()
        paths.add(mPath!!)
        colorsMap.put(mPath!!, selectedColor)
        widthMap.put(mPath!!, selectedWidth)
    }

    override fun onDraw(canvas: Canvas) {
        if (mPaint != null) {
            for (p in paths) {
                if (colorsMap[p] != null) {
                    mPaint!!.color = colorsMap[p]!!
                    mPaint!!.strokeWidth = widthMap[p]!!
                    canvas.drawPath(p, mPaint)
                }
            }
            mPaint!!.color = selectedColor
            mPaint!!.strokeWidth = selectedWidth
            canvas.drawPath(mPath!!, mPaint)
        }
    }

    fun changeColor(color: Int) {
        this.selectedColor = color
    }

    fun changeWidth(width: Float) {
        this.selectedWidth = width
    }

    private fun touchStart(x: Float, y: Float) {
        mPath!!.reset()
        mPath!!.moveTo(x, y)
        mX = x
        mY = y
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = Math.abs(x - mX)
        val dy = Math.abs(y - mY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
        }
    }

    private fun touchUp() {
        mPath!!.lineTo(mX, mY)
        // commit the path to our offscreen
        mCanvas!!.drawPath(mPath!!, mPaint!!)
        widthMap.put(mPath!!, selectedWidth)
        colorsMap.put(mPath!!, selectedColor)
        // kill this so we don't double draw
        mPath = Path()
        paths.add(mPath!!)
    }

    override fun onTouch(arg0: View, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
                invalidate()
            }
        }
        return true
    }

    fun onClickUndo() {
        if (paths.size > 1) {
            undonePaths.add(paths.removeAt(paths.size - 2))
            invalidate()
        }
    }

    fun onClickRedo() {
        if (undonePaths.size > 0) {
            paths.add(undonePaths.removeAt(undonePaths.size - 1))
            invalidate()
        }
    }

    @Throws(PackageManager.NameNotFoundException::class)
    fun saveCanvas(context: Context, player: Player) {
        val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.draw(canvas)

        val appDir = context.packageManager.getPackageInfo(context.packageName, 0).applicationInfo.dataDir
        val file = File(appDir, "drawing.jpg")

        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, FileOutputStream(file))
            "https://api.imgur.com/3/image".httpUpload()
                    .dataParts {_, _ ->
                        listOf(
                                DataPart(file, "image")
                        )
                    }
                    .header("Authorization" to "Client-ID ${resources.getString(R.string.imgur_api_key)}")
                    .responseJson { _, response, result ->
                Log.d(TAG, "Res: " + response.toString())
                Log.d(TAG, "Result: " + result.toString())
                result.fold(success = { json ->
                    val imageUrl = json.obj().getJSONObject("data").getString("link")
                    Log.d("imageUrl", imageUrl)
                    player.drawing = imageUrl
                    "/players".httpPut()
                            .body("{\"player\": ${Gson().toJson(player)}, \"stage\": 0}")
                            .header("Content-Type" to "application/json")
                            .responseJson { request, response, result ->
                                Log.d(TAG, "Req: " + request.toString())
                                Log.d(TAG, "Res: " + response.toString())
                                Log.d(TAG, "Result: " + result.toString())
                                result.fold(success = { json ->
                                    val playerObj = json.obj().getJSONObject("player")
                                    val newPlayer = Player(
                                            playerObj.getString("username"),
                                            playerObj.getString("token"),
                                            playerObj.getString("instanceID")
                                    )
                                    Log.d("player", newPlayer.username)
                                }, failure = { error ->
                                    Log.e("error", error.toString())
                                })
                            }
                }, failure = { error ->
                    Log.e("error", error.toString())
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private val TOUCH_TOLERANCE = 4f
    }

}