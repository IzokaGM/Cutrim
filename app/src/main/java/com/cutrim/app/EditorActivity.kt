package com.cutrim.app

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import java.util.Locale

class EditorActivity : Activity() {

    private lateinit var videoView: VideoView
    private lateinit var seekBar: SeekBar
    private lateinit var timeText: TextView
    private lateinit var markerText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var userSeeking = false
    private var trimStartMs = 0
    private var trimEndMs = 0
    private var splitMs = -1

    private val progressUpdater = object : Runnable {
        override fun run() {
            if (!userSeeking && ::videoView.isInitialized && videoView.duration > 0) {
                seekBar.max = videoView.duration
                seekBar.progress = videoView.currentPosition
                updateTime()
            }
            handler.postDelayed(this, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uriString = intent.getStringExtra(EXTRA_VIDEO_URI)
        if (uriString == null) {
            finish()
            return
        }

        fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
        val accent = Color.rgb(34, 230, 199)
        val background = Color.rgb(9, 14, 18)
        val panel = Color.rgb(18, 25, 30)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(14), dp(12), dp(10))
        }

        val back = Button(this).apply {
            text = "‹"
            textSize = 24f
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(panel)
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(48))
            setOnClickListener { finish() }
        }

        val title = TextView(this).apply {
            text = "CUTRIM EDITOR"
            textSize = 17f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
        }

        val export = Button(this).apply {
            text = "Export"
            isAllCaps = false
            setTextColor(Color.BLACK)
            backgroundTintList = ColorStateList.valueOf(accent)
            layoutParams = LinearLayout.LayoutParams(dp(92), dp(48))
            setOnClickListener {
                markerText.text = "Export comes next • editor controls are working"
            }
        }

        topBar.addView(back)
        topBar.addView(title)
        topBar.addView(export)

        videoView = VideoView(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(14))
            setBackgroundColor(panel)
        }

        timeText = TextView(this).apply {
            text = "00:00 / 00:00"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        seekBar = SeekBar(this).apply {
            progressTintList = ColorStateList.valueOf(accent)
            thumbTintList = ColorStateList.valueOf(accent)
        }

        val play = Button(this).apply {
            text = "▶ Play / Pause"
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.rgb(35, 45, 52))
            setOnClickListener {
                if (videoView.isPlaying) videoView.pause() else videoView.start()
            }
        }

        markerText = TextView(this).apply {
            text = "Trim: 00:00 → end"
            textSize = 12f
            setTextColor(Color.rgb(170, 180, 186))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        fun toolButton(label: String, action: () -> Unit) = Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.rgb(35, 45, 52))
            layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            setOnClickListener { action() }
        }

        toolbar.addView(toolButton("Trim Start") {
            trimStartMs = videoView.currentPosition
            if (trimEndMs in 1..trimStartMs) trimEndMs = 0
            updateMarkers()
        })

        toolbar.addView(toolButton("Trim End") {
            trimEndMs = videoView.currentPosition
            updateMarkers()
        })

        toolbar.addView(toolButton("Split") {
            splitMs = videoView.currentPosition
            updateMarkers()
        })

        controls.addView(timeText)
        controls.addView(seekBar)
        controls.addView(play)
        controls.addView(markerText)
        controls.addView(toolbar)

        root.addView(topBar)
        root.addView(videoView)
        root.addView(controls)
        setContentView(root)

        videoView.setVideoURI(Uri.parse(uriString))
        videoView.setOnPreparedListener { mp ->
            seekBar.max = mp.duration
            trimEndMs = mp.duration
            updateTime()
            updateMarkers()
            videoView.start()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoView.seekTo(progress)
                    updateTime(progress)
                }
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {
                userSeeking = true
            }

            override fun onStopTrackingTouch(bar: SeekBar?) {
                userSeeking = false
            }
        })

        handler.post(progressUpdater)
    }

    private fun updateTime(position: Int = videoView.currentPosition) {
        timeText.text = "${formatTime(position)} / ${formatTime(videoView.duration.coerceAtLeast(0))}"
    }

    private fun updateMarkers() {
        val end = if (trimEndMs > 0) formatTime(trimEndMs) else "end"
        val split = if (splitMs >= 0) " • Split ${formatTime(splitMs)}" else ""
        markerText.text = "Trim ${formatTime(trimStartMs)} → $end$split"
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = (ms.coerceAtLeast(0) / 1000)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        if (::videoView.isInitialized) videoView.pause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_VIDEO_URI = "video_uri"
    }
}
