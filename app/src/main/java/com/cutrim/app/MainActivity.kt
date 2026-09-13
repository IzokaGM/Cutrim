package com.cutrim.app

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView

class MainActivity : Activity() {

    private val backgroundColor = Color.rgb(8, 12, 16)
    private val surface = Color.rgb(18, 24, 30)
    private val surfaceAlt = Color.rgb(24, 31, 38)
    private val primary = Color.rgb(39, 226, 190)
    private val textPrimary = Color.rgb(244, 247, 249)
    private val textSecondary = Color.rgb(151, 162, 171)

    private val selectedVideos = mutableListOf<Uri>()

    private var selectedListContainer: LinearLayout? = null
    private var selectedCountText: TextView? = null
    private var createProjectButton: Button? = null

    private var screen = SCREEN_HOME

    private var videoView: VideoView? = null
    private var playButton: Button? = null
    private var seekBar: SeekBar? = null
    private var timeText: TextView? = null
    private var editorClipTitle: TextView? = null
    private var selectedClipIndex = 0
    private var userSeeking = false

    private val progressHandler = Handler(Looper.getMainLooper())

    private val progressUpdater = object : Runnable {
        override fun run() {
            val player = videoView

            if (screen == SCREEN_EDITOR && player != null) {
                val duration = player.duration.coerceAtLeast(0)
                val position = player.currentPosition.coerceAtLeast(0)

                if (!userSeeking && duration > 0) {
                    seekBar?.max = duration
                    seekBar?.progress = position
                }

                updateTime(position, duration)
                updatePlayButton()

                progressHandler.postDelayed(this, 250)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        showHome()
    }

    private fun showHome() {
        stopEditorUpdates()
        screen = SCREEN_HOME

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(26), dp(20), dp(32))
        }

        root.addView(buildHeader())
        root.addView(buildHero())
        root.addView(sectionTitle("Quick Start", 28))
        root.addView(buildQuickActions())
        root.addView(sectionTitle("Recent Projects", 30))
        root.addView(buildRecentProjects())

        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(scroll)
    }

    private fun showMediaImport() {
        stopEditorUpdates()
        screen = SCREEN_MEDIA

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(30))
        }

        root.addView(buildMediaHeader())

        root.addView(TextView(this).apply {
            text = "Choose your clips"
            textSize = 29f
            setTextColor(textPrimary)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(26)
            }
        })

        root.addView(TextView(this).apply {
            text = "Import one or more videos from your device."
            textSize = 14f
            setTextColor(textSecondary)

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(7)
            }
        })

        root.addView(Button(this).apply {
            text = "＋  Import Videos"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.rgb(4, 24, 21))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            backgroundTintList = ColorStateList.valueOf(primary)

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(24)
            }

            setOnClickListener {
                openVideoPicker()
            }
        })

        root.addView(buildSelectedHeader())

        selectedListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(
            selectedListContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        )

        createProjectButton = Button(this).apply {
            text = "Create Project"
            textSize = 16f
            isAllCaps = false
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            isEnabled = selectedVideos.isNotEmpty()

            updateCreateButtonStyle(this)

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(24)
            }

            setOnClickListener {
                if (selectedVideos.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Select at least one video.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    selectedClipIndex = 0
                    showEditor()
                }
            }
        }

        root.addView(createProjectButton)

        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(scroll)
        refreshSelectedVideos()
    }

    private fun showEditor() {
        if (selectedVideos.isEmpty()) {
            showMediaImport()
            return
        }

        stopEditorUpdates()
        screen = SCREEN_EDITOR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(22))
            setBackgroundColor(backgroundColor)
        }

        root.addView(buildEditorHeader())

        editorClipTitle = TextView(this).apply {
            textSize = 13f
            maxLines = 1
            setTextColor(textSecondary)
            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(8)
            }
        }

        root.addView(editorClipTitle)

        val previewCard = FrameLayout(this).apply {
            background = roundedBackground(Color.BLACK, 18)
        }

        videoView = VideoView(this).apply {
            setBackgroundColor(Color.BLACK)

            setOnPreparedListener {
                seekBar?.max = it.duration.coerceAtLeast(1)
                updateTime(0, it.duration)
                updatePlayButton()
            }

            setOnCompletionListener {
                seekTo(0)
                updatePlayButton()
                updateTime(0, duration.coerceAtLeast(0))
            }
        }

        previewCard.addView(
            videoView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            previewCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(300)
            )
        )

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

        playButton = Button(this).apply {
            text = "▶"
            textSize = 18f
            isAllCaps = false
            setTextColor(Color.rgb(4, 24, 21))
            backgroundTintList = ColorStateList.valueOf(primary)

            setOnClickListener {
                val player = videoView ?: return@setOnClickListener

                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.start()
                }

                updatePlayButton()
            }
        }

        controls.addView(
            playButton,
            LinearLayout.LayoutParams(dp(62), dp(48))
        )

        timeText = TextView(this).apply {
            text = "00:00 / 00:00"
            textSize = 13f
            setTextColor(textPrimary)
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            ).apply {
                marginStart = dp(12)
            }
        }

        controls.addView(timeText)
        root.addView(controls)

        seekBar = SeekBar(this).apply {
            max = 100
            progress = 0
            progressTintList = ColorStateList.valueOf(primary)
            thumbTintList = ColorStateList.valueOf(primary)

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    userSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val target = seekBar?.progress ?: 0
                    videoView?.seekTo(target)
                    userSeeking = false
                }

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        updateTime(
                            progress,
                            videoView?.duration?.coerceAtLeast(0) ?: 0
                        )
                    }
                }
            })
        }

        root.addView(
            seekBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        root.addView(sectionTitle("Timeline", 14))

        root.addView(
            buildTimeline(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(118)
            )
        )

        root.addView(TextView(this).apply {
            text = "Tap a clip to preview it. Trim and Split arrive in V4."
            textSize = 12f
            setTextColor(textSecondary)
            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        })

        root.addView(buildEditorToolbar())

        setContentView(root)

        loadClip(selectedClipIndex)
        startEditorUpdates()
    }

    private fun buildEditorHeader(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        row.addView(TextView(this).apply {
            text = "‹"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(textPrimary)
            isClickable = true
            isFocusable = true
            contentDescription = "Back"

            setOnClickListener {
                showMediaImport()
            }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))

        row.addView(TextView(this).apply {
            text = "Cutrim Editor"
            textSize = 20f
            setTextColor(textPrimary)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL

        }, LinearLayout.LayoutParams(
            0,
            dp(44),
            1f
        ))

        row.addView(TextView(this).apply {
            text = "V3"
            textSize = 12f
            setTextColor(primary)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER

        }, LinearLayout.LayoutParams(dp(52), dp(44)))

        return row
    }

    private fun buildTimeline(): View {
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        selectedVideos.forEachIndexed { index, uri ->
            row.addView(
                buildTimelineClip(uri, index),
                LinearLayout.LayoutParams(dp(128), dp(104)).apply {
                    marginEnd = dp(10)
                }
            )
        }

        scroll.addView(
            row,
            HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        return scroll
    }

    private fun buildTimelineClip(uri: Uri, index: Int): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(4), dp(4), dp(6))

            background = if (index == selectedClipIndex) {
                roundedBorderBackground(surfaceAlt, primary, 16, 2)
            } else {
                roundedBackground(surfaceAlt, 16)
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                selectedClipIndex = index
                loadClip(index)
                showEditor()
            }
        }

        val thumbnail = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(34, 42, 50))
            contentDescription = "Timeline clip thumbnail"
        }

        val bitmap = getVideoThumbnail(uri)

        if (bitmap != null) {
            thumbnail.setImageBitmap(bitmap)
        } else {
            thumbnail.setImageResource(android.R.drawable.ic_media_play)
            thumbnail.setPadding(dp(22), dp(22), dp(22), dp(22))
        }

        card.addView(
            thumbnail,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        card.addView(TextView(this).apply {
            text = "Clip ${index + 1}"
            textSize = 11f
            maxLines = 1
            gravity = Gravity.CENTER
            setTextColor(
                if (index == selectedClipIndex) primary else textSecondary
            )
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(26)
        ))

        return card
    }

    private fun buildEditorToolbar(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedBackground(surface, 18)

            setPadding(dp(8), dp(10), dp(8), dp(10))

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72)
            ).apply {
                topMargin = dp(18)
            }
        }

        row.addView(
            editorTool("✂", "Trim"),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        )

        row.addView(
            editorTool("Ⅱ", "Split"),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        )

        row.addView(
            editorTool("T", "Text"),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        )

        row.addView(
            editorTool("♪", "Audio"),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        )

        return row
    }

    private fun editorTool(symbol: String, label: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true

            addView(TextView(this@MainActivity).apply {
                text = symbol
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(textPrimary)
            })

            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(textSecondary)
            })

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "$label will be activated in a later milestone.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadClip(index: Int) {
        if (index !in selectedVideos.indices) return

        val uri = selectedVideos[index]
        editorClipTitle?.text = getDisplayName(uri)

        videoView?.apply {
            stopPlayback()
            setVideoURI(uri)
            seekTo(1)
        }

        seekBar?.progress = 0
        updatePlayButton()
    }

    private fun startEditorUpdates() {
        progressHandler.removeCallbacks(progressUpdater)
        progressHandler.post(progressUpdater)
    }

    private fun stopEditorUpdates() {
        progressHandler.removeCallbacks(progressUpdater)

        try {
            videoView?.pause()
        } catch (_: Exception) {
        }

        videoView = null
        playButton = null
        seekBar = null
        timeText = null
        editorClipTitle = null
    }

    private fun updatePlayButton() {
        playButton?.text = if (videoView?.isPlaying == true) "❚❚" else "▶"
    }

    private fun updateTime(positionMs: Int, durationMs: Int) {
        timeText?.text =
            "${formatTime(positionMs)} / ${formatTime(durationMs)}"
    }

    private fun formatTime(milliseconds: Int): String {
        val totalSeconds = (milliseconds.coerceAtLeast(0) / 1000)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun buildMediaHeader(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        row.addView(TextView(this).apply {
            text = "‹"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(textPrimary)
            isClickable = true
            isFocusable = true
            contentDescription = "Back"

            setOnClickListener {
                showHome()
            }

        }, LinearLayout.LayoutParams(dp(44), dp(44)))

        row.addView(TextView(this).apply {
            text = "Import Media"
            textSize = 20f
            setTextColor(textPrimary)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL

        }, LinearLayout.LayoutParams(
            0,
            dp(44),
            1f
        ))

        row.addView(TextView(this).apply {
            text = "CUTRIM"
            textSize = 12f
            setTextColor(primary)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL or Gravity.END

        }, LinearLayout.LayoutParams(
            dp(80),
            dp(44)
        ))

        return row
    }

    private fun buildSelectedHeader(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(30)
            }
        }

        row.addView(TextView(this).apply {
            text = "Selected Media"
            textSize = 19f
            setTextColor(textPrimary)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)

        }, LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        ))

        selectedCountText = TextView(this).apply {
            textSize = 13f
            setTextColor(primary)
            gravity = Gravity.END
        }

        row.addView(selectedCountText)

        return row
    }

    private fun refreshSelectedVideos() {
        selectedCountText?.text = "${selectedVideos.size} selected"
        selectedListContainer?.removeAllViews()

        if (selectedVideos.isEmpty()) {
            selectedListContainer?.addView(TextView(this).apply {
                text = "No videos selected yet."
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(textSecondary)
                setPadding(dp(12), dp(30), dp(12), dp(30))
                background = roundedBackground(surface, 18)
            })
        } else {
            selectedVideos.forEachIndexed { index, uri ->
                selectedListContainer?.addView(
                    buildSelectedVideoCard(uri, index),
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(92)
                    ).apply {
                        bottomMargin = dp(10)
                    }
                )
            }
        }

        createProjectButton?.isEnabled = selectedVideos.isNotEmpty()
        createProjectButton?.let { updateCreateButtonStyle(it) }
    }

    private fun buildSelectedVideoCard(uri: Uri, index: Int): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = roundedBackground(surfaceAlt, 16)
        }

        val thumbnail = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(34, 42, 50))
            contentDescription = "Video thumbnail"
        }

        val bitmap = getVideoThumbnail(uri)

        if (bitmap != null) {
            thumbnail.setImageBitmap(bitmap)
        } else {
            thumbnail.setImageResource(android.R.drawable.ic_media_play)
            thumbnail.setPadding(dp(22), dp(22), dp(22), dp(22))
        }

        row.addView(
            thumbnail,
            LinearLayout.LayoutParams(dp(72), dp(72))
        )

        val textWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
        }

        textWrap.addView(TextView(this).apply {
            text = getDisplayName(uri)
            textSize = 14f
            maxLines = 1
            setTextColor(textPrimary)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        })

        textWrap.addView(TextView(this).apply {
            text = "Video ready"
            textSize = 12f
            setTextColor(textSecondary)

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
            }
        })

        row.addView(
            textWrap,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        row.addView(TextView(this).apply {
            text = "×"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(textSecondary)
            isClickable = true
            isFocusable = true
            contentDescription = "Remove video"

            setOnClickListener {
                if (index in selectedVideos.indices) {
                    selectedVideos.removeAt(index)
                    refreshSelectedVideos()
                }
            }

        }, LinearLayout.LayoutParams(dp(44), dp(44)))

        return row
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        startActivityForResult(intent, REQUEST_VIDEO)
    }

    @Deprecated("Used for V3 without extra dependencies")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode != REQUEST_VIDEO ||
            resultCode != RESULT_OK ||
            data == null
        ) {
            return
        }

        val incoming = mutableListOf<Uri>()

        val clips = data.clipData

        if (clips != null) {
            for (i in 0 until clips.itemCount) {
                incoming.add(clips.getItemAt(i).uri)
            }
        } else {
            data.data?.let { incoming.add(it) }
        }

        incoming.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            if (!selectedVideos.contains(uri)) {
                selectedVideos.add(uri)
            }
        }

        if (screen != SCREEN_MEDIA) {
            showMediaImport()
        } else {
            refreshSelectedVideos()
        }
    }

    @Deprecated("Handled for current Activity UI")
    override fun onBackPressed() {
        when (screen) {
            SCREEN_EDITOR -> showMediaImport()
            SCREEN_MEDIA -> showHome()
            else -> super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()

        if (screen == SCREEN_EDITOR) {
            try {
                videoView?.pause()
                updatePlayButton()
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroy() {
        stopEditorUpdates()
        super.onDestroy()
    }

    private fun getDisplayName(uri: Uri): String {
        try {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                if (index >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(index) ?: "Video"
                }
            }
        } catch (_: Exception) {
        }

        return "Video"
    }

    private fun getVideoThumbnail(uri: Uri) = try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)

        val bitmap = retriever.getFrameAtTime(
            0,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )

        retriever.release()
        bitmap

    } catch (_: Exception) {
        null
    }

    private fun updateCreateButtonStyle(button: Button) {
        if (button.isEnabled) {
            button.setTextColor(Color.rgb(4, 24, 21))
            button.backgroundTintList = ColorStateList.valueOf(primary)
        } else {
            button.setTextColor(Color.rgb(125, 135, 141))
            button.backgroundTintList = ColorStateList.valueOf(
                Color.rgb(35, 42, 48)
            )
        }
    }

    private fun buildHeader(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        titleWrap.addView(TextView(this).apply {
            text = "CUTRIM"
            textSize = 29f
            setTextColor(textPrimary)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        })

        titleWrap.addView(TextView(this).apply {
            text = "Create. Edit. Share."
            textSize = 13f
            setTextColor(textSecondary)
        })

        row.addView(
            titleWrap,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        row.addView(TextView(this).apply {
            text = "●"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(primary)
            contentDescription = "Cutrim"
        })

        return row
    }

    private fun buildHero(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(24))

            background = roundedGradient(
                Color.rgb(20, 48, 49),
                Color.rgb(15, 27, 32),
                24
            )

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(28)
            }
        }

        card.addView(TextView(this).apply {
            text = "Your story,\nyour cut."
            textSize = 31f
            setTextColor(textPrimary)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setLineSpacing(0f, 0.96f)
        })

        card.addView(TextView(this).apply {
            text = "Start with a video from your device."
            textSize = 14f
            setTextColor(Color.rgb(189, 206, 207))

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        })

        card.addView(Button(this).apply {
            text = "＋  New Project"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.rgb(4, 24, 21))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            backgroundTintList = ColorStateList.valueOf(primary)

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(24)
            }

            setOnClickListener {
                showMediaImport()
            }
        })

        return card
    }

    private fun buildQuickActions(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }

        row.addView(
            actionCard(
                symbol = "▦",
                title = "Templates",
                subtitle = "Ready-made styles"
            ) {
                Toast.makeText(
                    this,
                    "Templates will be activated in a later milestone.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            LinearLayout.LayoutParams(0, dp(132), 1f).apply {
                marginEnd = dp(7)
            }
        )

        row.addView(
            actionCard(
                symbol = "＋",
                title = "New Project",
                subtitle = "Choose a video"
            ) {
                showMediaImport()
            },
            LinearLayout.LayoutParams(0, dp(132), 1f).apply {
                marginStart = dp(7)
            }
        )

        return row
    }

    private fun actionCard(
        symbol: String,
        title: String,
        subtitle: String,
        onClick: () -> Unit
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = roundedBackground(surfaceAlt, 18)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }

            addView(TextView(this@MainActivity).apply {
                text = symbol
                textSize = 24f
                setTextColor(primary)
            })

            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 16f
                setTextColor(textPrimary)
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(9)
                }
            })

            addView(TextView(this@MainActivity).apply {
                text = subtitle
                textSize = 12f
                setTextColor(textSecondary)

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(3)
                }
            })
        }
    }

    private fun buildRecentProjects(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(30), dp(20), dp(30))
            background = roundedBackground(surface, 20)

            addView(TextView(this@MainActivity).apply {
                text = "▶"
                textSize = 27f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(95, 109, 119))
            })

            addView(TextView(this@MainActivity).apply {
                text = "No projects yet"
                textSize = 17f
                gravity = Gravity.CENTER
                setTextColor(textPrimary)
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(10)
                }
            })

            addView(TextView(this@MainActivity).apply {
                text = "Your saved projects will appear here."
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(textSecondary)

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(5)
                }
            })
        }
    }

    private fun sectionTitle(
        text: String,
        topMargin: Int
    ): View {
        return TextView(this).apply {
            this.text = text
            textSize = 19f
            setTextColor(textPrimary)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                this.topMargin = dp(topMargin)
                bottomMargin = dp(12)
            }
        }
    }

    private fun roundedBackground(
        color: Int,
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
        }
    }

    private fun roundedBorderBackground(
        color: Int,
        strokeColor: Int,
        radiusDp: Int,
        strokeDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
            setStroke(dp(strokeDp), strokeColor)
        }
    }

    private fun roundedGradient(
        startColor: Int,
        endColor: Int,
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(startColor, endColor)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val REQUEST_VIDEO = 1001
        private const val SCREEN_HOME = 0
        private const val SCREEN_MEDIA = 1
        private const val SCREEN_EDITOR = 2
    }
}
