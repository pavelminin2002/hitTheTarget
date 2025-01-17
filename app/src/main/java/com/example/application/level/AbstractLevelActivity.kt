package com.example.application.level

import android.graphics.Canvas
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import androidx.annotation.RequiresApi
import com.example.application.MyActivity
import com.example.application.R
import com.example.application.level.figures.FigureBase


open class AbstractLevelActivity : MyActivity(), SurfaceHolder.Callback2 {

    open var FPS = 30
    open var frameDurationMs = 1000 / FPS

    private val lock = Object()
    @Volatile var canRun = false
    @Volatile var threadQuit = false

    lateinit var surface: SurfaceView

    var WIDTH = 0
    var HEIGHT = 0

    var figures = ArrayDeque<FigureBase>()
    var needRedraw = true

    @RequiresApi(Build.VERSION_CODES.O)
    private var drawingThread = Thread {
        while (!threadQuit) {
            if (!canRun) {
                synchronized(lock) {
                    try {
                        lock.wait()
                    } catch (e: Exception) {
                        // pass
                    }
                }
            }
            val startTime = System.currentTimeMillis()
            try {
                // Start drawing
                handleNewFrame(startTime)
                val drawTime = System.currentTimeMillis() - startTime

                if (drawTime < frameDurationMs) {
                    Thread.sleep(frameDurationMs - drawTime)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun drawNewFrame() {
        val canvas = surface.holder.lockCanvas()
        if (canvas != null) {
            setBackgroundColor(canvas)
            // canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            for (figure in figures) {
                figure.draw(canvas)
            }
        }
        surface.holder.unlockCanvasAndPost(canvas)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleNewFrame(startTime: Long) {
        calculateNewFrame(startTime)  //
        onHandlingNewFrame()
        if (needRedraw) {
            drawNewFrame()
            needRedraw = false
        }
    }


    open fun setBackgroundColor(canvas: Canvas) {
        canvas.drawColor(settings.backgroundColor.rgb.toInt())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        frameDurationMs = 1000 / FPS

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        loadContentView()

        surface = findViewById(R.id.surface)
        surface.holder.addCallback(this)
        surface.holder.setFormat(PixelFormat.RGBA_8888)
        drawingThread.start()

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        HEIGHT = displayMetrics.heightPixels
        WIDTH = displayMetrics.widthPixels
    }

    override fun onDestroy() {
        super.onDestroy()
        threadQuit = true
        canRun = true
        try {
            synchronized(lock) {
                lock.notify()
            }
        } catch (e: Exception) {
            // pass
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        /** Always returns true */

        if (event === null) return true

        var touchReached = false
        for (figure in figures.reversed()) {
            if (figure.handleTouchEvent(event, touchReached)) {
                touchReached = true
            }
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        threadQuit = false
        canRun = true
        try {
            synchronized(lock) {
                lock.notify()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onLevelStart()
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        // pass
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        canRun = false
    }

    override fun surfaceRedrawNeeded(p0: SurfaceHolder) {
        // pass
    }

    open fun setNewActiveFigure() {
        /** When player passed last active figure (clicked on it),
         *  and we need to get new active figure (and to delete last active figure)
         *  */
        figures.removeLast()
        if (figures.isNotEmpty()) {
            figures.last().setActive()
            needRedraw = true
        } else {
            finishLevel()
        }
    }

    /** Ниже функции, которые можно переопределять для описания своего уровня */

    open fun onHandlingNewFrame() {
        // redefine this
        // called on creating new frame - after calculating and before drawing
    }

    open fun finishLevel() {
        // redefine this
        // For example, show modal with results.

        threadQuit = true
        finish() // close LevelActivity
    }

    open fun loadContentView() {
        // You can define your custom xml layout for settings here
        setContentView(R.layout.activity_abstract_level)

        // redefine this
    }

    open fun calculateNewFrame(startTime: Long) {
        /** Make calculations before drawing new frame (for example, if we need to move figure) */
        for (figure in figures.reversed()) {
            figure.calculateNewFrame(startTime)
            if (figure.needRedraw) {
                needRedraw = true
                figure.needRedraw = false
            }
        }
    }

    open fun onLevelStart() {
        /** Called when the first frame started to be drawn */

        // define this
    }
}