package com.cutrim.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.Manifest
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import android.widget.VideoView

class MainActivity : Activity() {

    data class ClipSegment(
        val uri: Uri,
        var startMs: Int,
        var endMs: Int,
        val name: String,
        var filterName: String = "None",
        var transitionName: String = "None",
        var effectName: String = "None",
        var speed: Float = 1f,
        var stickers: List<String> = emptyList()
    ) {
        fun lengthMs(): Int = (endMs - startMs).coerceAtLeast(1)
    }

    data class EditorState(
        val clips: List<ClipSegment>,
        val selectedIndex: Int
    )

    data class TextOverlay(
        var text: String,
        var sizeSp: Float = 26f,
        var color: Int = Color.WHITE
    )

    data class AudioTrack(
        val uri: Uri,
        val name: String,
        val type: String,
        var volume: Float = 1f,
        var fadeIn: Boolean = false,
        var fadeOut: Boolean = false
    )

    private val backgroundColor = Color.rgb(8, 12, 16)
    private val surface = Color.rgb(18, 24, 30)
    private val surfaceAlt = Color.rgb(24, 31, 38)
    private val primary = Color.rgb(39, 226, 190)
    private val textPrimary = Color.rgb(244, 247, 249)
    private val textSecondary = Color.rgb(151, 162, 171)

    private val selectedVideos = mutableListOf<Uri>()
    private val editorClips = mutableListOf<ClipSegment>()

    private val textOverlays = mutableListOf<TextOverlay>()
    private val audioTracks = mutableListOf<AudioTrack>()

    private val undoStack = mutableListOf<EditorState>()
    private val redoStack = mutableListOf<EditorState>()

    private var selectedListContainer: LinearLayout? = null
    private var selectedCountText: TextView? = null
    private var createProjectButton: Button? = null

    private var screen = SCREEN_HOME

    private var videoView: VideoView? = null
    private var playButton: Button? = null
    private var seekBar: SeekBar? = null
    private var timeText: TextView? = null
    private var editorClipTitle: TextView? = null
    private var overlayLayer: FrameLayout? = null
    private var filterOverlay: View? = null
    private var creativeStatusText: TextView? = null
    private var audioStatusText: TextView? = null
    private var audioVolumeSeek: SeekBar? = null
    private var audioPlayer: MediaPlayer? = null
    private var selectedClipIndex = 0
    private var selectedAudioIndex = -1
    private var pendingAudioType = "Music"
    private var userSeeking = false

    private val progressHandler = Handler(Looper.getMainLooper())

    private val progressUpdater = object : Runnable {
        override fun run() {
            val player = videoView
            val clip = currentClip()

            if (screen == SCREEN_EDITOR && player != null && clip != null) {
                val absolutePosition = player.currentPosition.coerceAtLeast(0)

                if (player.isPlaying && absolutePosition >= clip.endMs) {
                    player.pause()
                    player.seekTo(clip.endMs)
                }

                val relativePosition =
                    (player.currentPosition - clip.startMs)
                        .coerceIn(0, clip.lengthMs())

                if (!userSeeking) {
                    seekBar?.max = clip.lengthMs()
                    seekBar?.progress = relativePosition
                }

                updateTime(relativePosition, clip.lengthMs())
                updatePlayButton()

                progressHandler.postDelayed(this, 200)
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

            setOnClickListener { openVideoPicker() }
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
                    toast("Select at least one video.")
                } else {
                    createEditorProject()
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

    private fun createEditorProject() {
        editorClips.clear()

        selectedVideos.forEach { uri ->
            val duration = getVideoDuration(uri).coerceAtLeast(MIN_CLIP_MS * 2)

            editorClips.add(
                ClipSegment(
                    uri = uri,
                    startMs = 0,
                    endMs = duration,
                    name = getDisplayName(uri)
                )
            )
        }

        undoStack.clear()
        redoStack.clear()
        textOverlays.clear()
        audioTracks.clear()
        selectedAudioIndex = -1
        selectedClipIndex = 0

        showEditor()
    }

    private fun showEditor() {
        if (editorClips.isEmpty()) {
            showMediaImport()
            return
        }

        stopEditorUpdates()
        screen = SCREEN_EDITOR

        selectedClipIndex =
            selectedClipIndex.coerceIn(0, editorClips.lastIndex)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(20))
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
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
        }

        root.addView(editorClipTitle)

        val previewCard = FrameLayout(this).apply {
            background = roundedBackground(Color.BLACK, 18)
        }

        videoView = VideoView(this).apply {
            setBackgroundColor(Color.BLACK)

            setOnPreparedListener { mediaPlayer ->
                val clip = currentClip() ?: return@setOnPreparedListener

                try {
                    mediaPlayer.playbackParams =
                        mediaPlayer.playbackParams.setSpeed(clip.speed)
                } catch (_: Exception) {
                }

                seekBar?.max = clip.lengthMs()
                seekBar?.progress = 0

                seekTo(clip.startMs)
                updateTime(0, clip.lengthMs())
                updatePlayButton()
            }

            setOnCompletionListener {
                val clip = currentClip() ?: return@setOnCompletionListener

                seekTo(clip.startMs)
                updatePlayButton()
                updateTime(0, clip.lengthMs())
            }
        }

        previewCard.addView(
            videoView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        filterOverlay = View(this)
        previewCard.addView(
            filterOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        overlayLayer = FrameLayout(this)
        previewCard.addView(
            overlayLayer,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        refreshCreativePreview()
        refreshTextOverlays()

        root.addView(
            previewCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(270)
            )
        )

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
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
                val clip = currentClip() ?: return@setOnClickListener

                if (player.isPlaying) {
                    player.pause()
                } else {
                    if (player.currentPosition >= clip.endMs - 80) {
                        player.seekTo(clip.startMs)
                    }

                    player.start()
                }

                updatePlayButton()
            }
        }

        controls.addView(
            playButton,
            LinearLayout.LayoutParams(dp(62), dp(46))
        )

        timeText = TextView(this).apply {
            text = "00:00 / 00:00"
            textSize = 13f
            setTextColor(textPrimary)
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                0,
                dp(46),
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
                    val clip = currentClip()
                    val relative = seekBar?.progress ?: 0

                    if (clip != null) {
                        videoView?.seekTo(clip.startMs + relative)
                    }

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
                            currentClip()?.lengthMs() ?: 0
                        )
                    }
                }
            })
        }

        root.addView(
            seekBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        root.addView(sectionTitle("Timeline", 8))

        root.addView(
            buildTimeline(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(104)
            )
        )

        root.addView(
            buildEditActions(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        )

        root.addView(sectionTitle("Creative Tools", 12))
        root.addView(buildCreativeTools())

        root.addView(sectionTitle("Text", 12))
        root.addView(buildTextTools())

        root.addView(sectionTitle("Audio", 12))
        root.addView(buildAudioTools())

        root.addView(TextView(this).apply {
            text = "Creative, text and audio settings are stored in the project. Final render arrives in V7."
            textSize = 12f
            setTextColor(textSecondary)
            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        })

        setContentView(root)

        loadClip(selectedClipIndex)
        startEditorUpdates()
    }

    private fun buildEditActions(): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(surface, 18)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        row1.addView(
            actionButton("Trim Start") { trimStart() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(4)
            }
        )

        row1.addView(
            actionButton("Trim End") { trimEnd() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        row1.addView(
            actionButton("Split") { splitClip() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        row1.addView(
            actionButton("Delete") { deleteClip() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
            }
        )

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }

        row2.addView(
            actionButton("← Move") { moveClip(-1) },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(4)
            }
        )

        row2.addView(
            actionButton("Move →") { moveClip(1) },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        row2.addView(
            actionButton("Undo") { undoEdit() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        row2.addView(
            actionButton("Redo") { redoEdit() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
            }
        )

        wrap.addView(row1)
        wrap.addView(row2)

        return wrap
    }

    private fun actionButton(
        label: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setTextColor(textPrimary)
            backgroundTintList = ColorStateList.valueOf(surfaceAlt)
            setPadding(dp(3), 0, dp(3), 0)

            setOnClickListener { action() }
        }
    }

    private fun trimStart() {
        val clip = currentClip() ?: return
        val position = videoView?.currentPosition ?: clip.startMs

        if (position <= clip.startMs + MIN_CLIP_MS) {
            toast("Move the playhead further right first.")
            return
        }

        if (position >= clip.endMs - MIN_CLIP_MS) {
            toast("Trim would make the clip too short.")
            return
        }

        pushUndo()
        clip.startMs = position
        loadClip(selectedClipIndex)
        toast("Trim start applied.")
    }

    private fun trimEnd() {
        val clip = currentClip() ?: return
        val position = videoView?.currentPosition ?: clip.endMs

        if (position >= clip.endMs - MIN_CLIP_MS) {
            toast("Move the playhead left first.")
            return
        }

        if (position <= clip.startMs + MIN_CLIP_MS) {
            toast("Trim would make the clip too short.")
            return
        }

        pushUndo()
        clip.endMs = position
        loadClip(selectedClipIndex)
        toast("Trim end applied.")
    }

    private fun splitClip() {
        val clip = currentClip() ?: return
        val position = videoView?.currentPosition ?: clip.startMs

        if (
            position <= clip.startMs + MIN_CLIP_MS ||
            position >= clip.endMs - MIN_CLIP_MS
        ) {
            toast("Move the playhead away from the clip edges.")
            return
        }

        pushUndo()

        val left = clip.copy(endMs = position)
        val right = clip.copy(startMs = position)

        editorClips[selectedClipIndex] = left
        editorClips.add(selectedClipIndex + 1, right)

        selectedClipIndex += 1
        showEditor()
        toast("Clip split.")
    }

    private fun deleteClip() {
        if (editorClips.isEmpty()) return

        pushUndo()
        editorClips.removeAt(selectedClipIndex)

        if (editorClips.isEmpty()) {
            toast("Timeline is empty.")
            showMediaImport()
            return
        }

        selectedClipIndex =
            selectedClipIndex.coerceAtMost(editorClips.lastIndex)

        showEditor()
    }

    private fun moveClip(direction: Int) {
        val target = selectedClipIndex + direction

        if (target !in editorClips.indices) {
            toast("Clip cannot move further.")
            return
        }

        pushUndo()

        val moving = editorClips.removeAt(selectedClipIndex)
        editorClips.add(target, moving)
        selectedClipIndex = target

        showEditor()
    }

    private fun pushUndo() {
        undoStack.add(snapshotState())

        if (undoStack.size > MAX_HISTORY) {
            undoStack.removeAt(0)
        }

        redoStack.clear()
    }

    private fun undoEdit() {
        if (undoStack.isEmpty()) {
            toast("Nothing to undo.")
            return
        }

        redoStack.add(snapshotState())
        restoreState(undoStack.removeAt(undoStack.lastIndex))
        showEditor()
    }

    private fun redoEdit() {
        if (redoStack.isEmpty()) {
            toast("Nothing to redo.")
            return
        }

        undoStack.add(snapshotState())
        restoreState(redoStack.removeAt(redoStack.lastIndex))
        showEditor()
    }

    private fun snapshotState(): EditorState {
        return EditorState(
            clips = editorClips.map { it.copy(stickers = it.stickers.toList()) },
            selectedIndex = selectedClipIndex
        )
    }

    private fun restoreState(state: EditorState) {
        editorClips.clear()
        editorClips.addAll(state.clips.map { it.copy(stickers = it.stickers.toList()) })

        selectedClipIndex =
            if (editorClips.isEmpty()) {
                0
            } else {
                state.selectedIndex.coerceIn(0, editorClips.lastIndex)
            }
    }

    private fun currentClip(): ClipSegment? {
        return editorClips.getOrNull(selectedClipIndex)
    }



    private fun buildCreativeTools(): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(surface, 18)
            setPadding(dp(8), dp(8), dp(8), dp(10))
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row1.addView(
            actionButton("Filter") { cycleFilter() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(4)
            }
        )

        row1.addView(
            actionButton("Transition") { cycleTransition() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        row1.addView(
            actionButton("Effect") { cycleEffect() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        row1.addView(
            actionButton("Sticker") { addSticker() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
            }
        )

        wrap.addView(row1)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }

        row2.addView(
            actionButton("Speed -") { changeSpeed(-1) },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(4)
            }
        )

        row2.addView(
            actionButton("Speed +") { changeSpeed(1) },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        row2.addView(
            actionButton("Remove Sticker") { removeSticker() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
            }
        )

        wrap.addView(row2)

        creativeStatusText = TextView(this).apply {
            text = currentCreativeLabel()
            textSize = 12f
            setTextColor(textSecondary)
            setPadding(dp(4), dp(10), dp(4), dp(2))
        }

        wrap.addView(creativeStatusText)
        return wrap
    }

    private fun cycleFilter() {
        val clip = currentClip() ?: return
        pushUndo()

        val options = listOf("None", "Warm", "Cool", "Mono", "Vintage")
        val current = options.indexOf(clip.filterName).coerceAtLeast(0)
        clip.filterName = options[(current + 1) % options.size]

        refreshCreativePreview()
        refreshCreativeStatus()
    }

    private fun cycleTransition() {
        val clip = currentClip() ?: return
        pushUndo()

        val options = listOf("None", "Fade", "Dissolve", "Slide", "Zoom")
        val current = options.indexOf(clip.transitionName).coerceAtLeast(0)
        clip.transitionName = options[(current + 1) % options.size]

        refreshCreativeStatus()
    }

    private fun cycleEffect() {
        val clip = currentClip() ?: return
        pushUndo()

        val options = listOf("None", "Flash", "Dream", "Retro", "Cinema")
        val current = options.indexOf(clip.effectName).coerceAtLeast(0)
        clip.effectName = options[(current + 1) % options.size]

        refreshCreativePreview()
        refreshCreativeStatus()
    }

    private fun addSticker() {
        val clip = currentClip() ?: return
        pushUndo()

        val options = listOf("✨", "❤️", "🔥", "😎", "🎬")
        val next = options[clip.stickers.size % options.size]
        clip.stickers = clip.stickers + next

        refreshCreativePreview()
        refreshCreativeStatus()
    }

    private fun removeSticker() {
        val clip = currentClip() ?: return

        if (clip.stickers.isEmpty()) {
            toast("No sticker on this clip.")
            return
        }

        pushUndo()
        clip.stickers = clip.stickers.dropLast(1)

        refreshCreativePreview()
        refreshCreativeStatus()
    }

    private fun changeSpeed(direction: Int) {
        val clip = currentClip() ?: return
        val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

        var current = speeds.indexOfFirst {
            kotlin.math.abs(it - clip.speed) < 0.01f
        }
        if (current < 0) current = 2

        val next = (current + direction).coerceIn(0, speeds.lastIndex)

        if (next == current) {
            toast("Speed limit reached.")
            return
        }

        pushUndo()
        clip.speed = speeds[next]

        refreshCreativeStatus()
        loadClip(selectedClipIndex)
    }

    private fun currentCreativeLabel(): String {
        val clip = currentClip() ?: return "No clip selected"

        return "Filter: ${clip.filterName}  •  " +
            "Transition: ${clip.transitionName}  •  " +
            "Effect: ${clip.effectName}  •  " +
            "Speed: ${clip.speed}x  •  " +
            "Stickers: ${clip.stickers.size}"
    }

    private fun refreshCreativeStatus() {
        creativeStatusText?.text = currentCreativeLabel()
    }

    private fun refreshCreativePreview() {
        val clip = currentClip() ?: return

        val tint = when (clip.filterName) {
            "Warm" -> Color.argb(55, 255, 120, 45)
            "Cool" -> Color.argb(55, 45, 130, 255)
            "Mono" -> Color.argb(75, 110, 110, 110)
            "Vintage" -> Color.argb(60, 150, 105, 55)
            else -> Color.TRANSPARENT
        }

        filterOverlay?.setBackgroundColor(tint)

        val layer = overlayLayer ?: return
        val removeViews = mutableListOf<View>()

        for (i in 0 until layer.childCount) {
            val child = layer.getChildAt(i)
            if (child.tag == "creative") {
                removeViews.add(child)
            }
        }

        removeViews.forEach { layer.removeView(it) }

        clip.stickers.forEachIndexed { index, sticker ->
            val stickerView = TextView(this).apply {
                text = sticker
                textSize = 28f
                tag = "creative"
                gravity = Gravity.CENTER
            }

            val horizontal =
                if (index % 2 == 0) Gravity.START else Gravity.END
            val vertical =
                if ((index / 2) % 2 == 0) Gravity.TOP else Gravity.BOTTOM

            layer.addView(
                stickerView,
                FrameLayout.LayoutParams(
                    dp(64),
                    dp(64),
                    horizontal or vertical
                ).apply {
                    leftMargin = dp(14)
                    rightMargin = dp(14)
                    topMargin = dp(14)
                    bottomMargin = dp(14)
                }
            )
        }

        if (clip.effectName != "None") {
            layer.addView(
                TextView(this).apply {
                    text = clip.effectName.uppercase()
                    textSize = 10f
                    setTextColor(Color.WHITE)
                    setPadding(dp(7), dp(4), dp(7), dp(4))
                    background = roundedBackground(
                        Color.argb(120, 0, 0, 0),
                        6
                    )
                    tag = "creative"
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.END or Gravity.TOP
                ).apply {
                    topMargin = dp(10)
                    rightMargin = dp(10)
                }
            )
        }
    }

    private fun buildTextTools(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedBackground(surface, 18)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        row.addView(
            actionButton("Add Text") { showTextDialog(null) },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(4)
            }
        )

        row.addView(
            actionButton("Edit Last") {
                if (textOverlays.isEmpty()) {
                    toast("No text overlay yet.")
                } else {
                    showTextDialog(textOverlays.lastIndex)
                }
            },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        row.addView(
            actionButton("Delete Last") {
                if (textOverlays.isEmpty()) {
                    toast("No text overlay yet.")
                } else {
                    textOverlays.removeAt(textOverlays.lastIndex)
                    refreshTextOverlays()
                    toast("Text removed.")
                }
            },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
            }
        )

        return row
    }

    private fun showTextDialog(index: Int?) {
        val existing = index?.let { textOverlays.getOrNull(it) }

        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), 0)
        }

        val input = EditText(this).apply {
            hint = "Enter text"
            setText(existing?.text ?: "")
            setSingleLine(false)
        }

        wrap.addView(input)

        val sizeSeek = SeekBar(this).apply {
            min = 14
            max = 54
            progress = existing?.sizeSp?.toInt() ?: 26
        }

        wrap.addView(TextView(this).apply {
            text = "Text size"
            setPadding(0, dp(12), 0, 0)
        })
        wrap.addView(sizeSeek)

        var selectedColor = existing?.color ?: Color.WHITE

        val colorRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val colors = listOf(
            "White" to Color.WHITE,
            "Teal" to primary,
            "Yellow" to Color.YELLOW,
            "Red" to Color.RED
        )

        colors.forEach { pair ->
            colorRow.addView(Button(this).apply {
                text = pair.first
                isAllCaps = false
                textSize = 11f
                setOnClickListener {
                    selectedColor = pair.second
                    toast("${pair.first} selected.")
                }
            }, LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
            ))
        }

        wrap.addView(colorRow)

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Text" else "Edit Text")
            .setView(wrap)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim()

                if (value.isEmpty()) {
                    toast("Text cannot be empty.")
                    return@setPositiveButton
                }

                if (existing == null) {
                    textOverlays.add(
                        TextOverlay(
                            text = value,
                            sizeSp = sizeSeek.progress.toFloat(),
                            color = selectedColor
                        )
                    )
                } else {
                    existing.text = value
                    existing.sizeSp = sizeSeek.progress.toFloat()
                    existing.color = selectedColor
                }

                refreshTextOverlays()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshTextOverlays() {
        val layer = overlayLayer ?: return
        layer.removeAllViews()

        textOverlays.forEachIndexed { index, overlay ->
            val view = TextView(this).apply {
                text = overlay.text
                textSize = overlay.sizeSp
                setTextColor(overlay.color)
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(6), dp(8), dp(6))
                background = roundedBackground(
                    Color.argb(90, 0, 0, 0),
                    8
                )
            }

            layer.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                ).apply {
                    topMargin = index * dp(46)
                }
            )
        }

        refreshCreativePreview()
    }

    private fun buildAudioTools(): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(surface, 18)
            setPadding(dp(8), dp(8), dp(8), dp(10))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row.addView(
            actionButton("Music") {
                pendingAudioType = "Music"
                openAudioPicker()
            },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(4)
            }
        )

        row.addView(
            actionButton("Voice") {
                pendingAudioType = "Voice"
                openAudioPicker()
            },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        row.addView(
            actionButton("Play Audio") {
                toggleAudioPreview()
            },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(4)
            }
        )

        wrap.addView(row)

        audioStatusText = TextView(this).apply {
            text = currentAudioLabel()
            textSize = 12f
            setTextColor(textSecondary)
            setPadding(dp(4), dp(10), dp(4), dp(4))
        }

        wrap.addView(audioStatusText)

        audioVolumeSeek = SeekBar(this).apply {
            min = 0
            max = 100
            progress = currentAudio()?.let { (it.volume * 100).toInt() } ?: 100

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        currentAudio()?.volume = progress / 100f
                        audioPlayer?.setVolume(
                            progress / 100f,
                            progress / 100f
                        )
                        refreshAudioStatus()
                    }
                }
            })
        }

        wrap.addView(TextView(this).apply {
            text = "Volume"
            textSize = 12f
            setTextColor(textSecondary)
            setPadding(dp(4), dp(6), dp(4), 0)
        })
        wrap.addView(audioVolumeSeek)

        val fadeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fadeRow.addView(
            actionButton("Fade In") {
                val track = currentAudio()
                if (track == null) {
                    toast("Import audio first.")
                } else {
                    track.fadeIn = !track.fadeIn
                    refreshAudioStatus()
                }
            },
            LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                marginEnd = dp(4)
            }
        )

        fadeRow.addView(
            actionButton("Fade Out") {
                val track = currentAudio()
                if (track == null) {
                    toast("Import audio first.")
                } else {
                    track.fadeOut = !track.fadeOut
                    refreshAudioStatus()
                }
            },
            LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        fadeRow.addView(
            actionButton("Remove") {
                if (selectedAudioIndex in audioTracks.indices) {
                    stopAudioPreview()
                    audioTracks.removeAt(selectedAudioIndex)
                    selectedAudioIndex =
                        if (audioTracks.isEmpty()) -1 else audioTracks.lastIndex
                    refreshAudioStatus()
                    toast("Audio removed.")
                } else {
                    toast("No audio selected.")
                }
            },
            LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                marginStart = dp(4)
            }
        )

        wrap.addView(fadeRow)

        return wrap
    }

    private fun openAudioPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }

        startActivityForResult(intent, REQUEST_AUDIO)
    }

    private fun currentAudio(): AudioTrack? {
        return audioTracks.getOrNull(selectedAudioIndex)
    }

    private fun currentAudioLabel(): String {
        val track = currentAudio() ?: return "No audio track selected"

        val fadeParts = mutableListOf<String>()
        if (track.fadeIn) fadeParts.add("Fade In")
        if (track.fadeOut) fadeParts.add("Fade Out")
        val fade = fadeParts.joinToString(" + ")

        return buildString {
            append("${track.type}: ${track.name}")
            append("  •  ${(track.volume * 100).toInt()}%")
            if (fade.isNotEmpty()) {
                append("  •  $fade")
            }
        }
    }

    private fun refreshAudioStatus() {
        audioStatusText?.text = currentAudioLabel()
        audioVolumeSeek?.progress =
            currentAudio()?.let { (it.volume * 100).toInt() } ?: 100
    }

    private fun toggleAudioPreview() {
        val track = currentAudio()

        if (track == null) {
            toast("Import music or voice audio first.")
            return
        }

        if (audioPlayer?.isPlaying == true) {
            audioPlayer?.pause()
            return
        }

        if (audioPlayer == null) {
            try {
                audioPlayer = MediaPlayer().apply {
                    setDataSource(this@MainActivity, track.uri)
                    prepare()
                    setVolume(track.volume, track.volume)
                }
            } catch (_: Exception) {
                stopAudioPreview()
                toast("Unable to play this audio.")
                return
            }
        }

        audioPlayer?.start()
    }

    private fun stopAudioPreview() {
        try {
            audioPlayer?.stop()
        } catch (_: Exception) {
        }

        try {
            audioPlayer?.release()
        } catch (_: Exception) {
        }

        audioPlayer = null
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

            setOnClickListener { showMediaImport() }

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
            text = "V6"
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

        editorClips.forEachIndexed { index, clip ->
            row.addView(
                buildTimelineClip(clip, index),
                LinearLayout.LayoutParams(dp(128), dp(94)).apply {
                    marginEnd = dp(10)
                }
            )
        }

        scroll.addView(
            row,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        return scroll
    }

    private fun buildTimelineClip(
        clip: ClipSegment,
        index: Int
    ): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(4), dp(4), dp(5))

            background = if (index == selectedClipIndex) {
                roundedBorderBackground(surfaceAlt, primary, 16, 2)
            } else {
                roundedBackground(surfaceAlt, 16)
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                selectedClipIndex = index
                showEditor()
            }
        }

        val thumbnail = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(34, 42, 50))
            contentDescription = "Timeline clip thumbnail"
        }

        val bitmap = getVideoThumbnail(clip.uri, clip.startMs)

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
                dp(60)
            )
        )

        card.addView(TextView(this).apply {
            text = "${index + 1}  ${formatTime(clip.lengthMs())}"
            textSize = 10f
            maxLines = 1
            gravity = Gravity.CENTER
            setTextColor(
                if (index == selectedClipIndex) primary else textSecondary
            )

        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(25)
        ))

        return card
    }

    private fun loadClip(index: Int) {
        val clip = editorClips.getOrNull(index) ?: return

        editorClipTitle?.text =
            "${clip.name}  •  ${formatTime(clip.lengthMs())}"

        refreshCreativePreview()
        refreshCreativeStatus()

        videoView?.apply {
            stopPlayback()
            setVideoURI(clip.uri)
        }

        seekBar?.max = clip.lengthMs()
        seekBar?.progress = 0

        updateTime(0, clip.lengthMs())
        updatePlayButton()
    }

    private fun startEditorUpdates() {
        progressHandler.removeCallbacks(progressUpdater)
        progressHandler.post(progressUpdater)
    }

    private fun stopEditorUpdates() {
        progressHandler.removeCallbacks(progressUpdater)
        stopAudioPreview()

        try {
            videoView?.pause()
        } catch (_: Exception) {
        }

        videoView = null
        playButton = null
        seekBar = null
        timeText = null
        editorClipTitle = null
        overlayLayer = null
        filterOverlay = null
        creativeStatusText = null
        audioStatusText = null
        audioVolumeSeek = null
    }

    private fun updatePlayButton() {
        playButton?.text =
            if (videoView?.isPlaying == true) "❚❚" else "▶"
    }

    private fun updateTime(
        positionMs: Int,
        durationMs: Int
    ) {
        timeText?.text =
            "${formatTime(positionMs)} / ${formatTime(durationMs)}"
    }

    private fun formatTime(milliseconds: Int): String {
        val totalSeconds = milliseconds.coerceAtLeast(0) / 1000
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

            setOnClickListener { showHome() }

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

    private fun buildSelectedVideoCard(
        uri: Uri,
        index: Int
    ): View {
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

        val bitmap = getVideoThumbnail(uri, 0)

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
            text = formatTime(getVideoDuration(uri))
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

    @Deprecated("Used without extra dependencies")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_AUDIO) {
            if (resultCode != RESULT_OK || data?.data == null) {
                return
            }

            val uri = data.data ?: return

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            audioTracks.add(
                AudioTrack(
                    uri = uri,
                    name = getDisplayName(uri),
                    type = pendingAudioType
                )
            )

            selectedAudioIndex = audioTracks.lastIndex
            stopAudioPreview()
            refreshAudioStatus()
            toast("$pendingAudioType added.")
            return
        }

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
                val index =
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                if (index >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(index) ?: "Video"
                }
            }
        } catch (_: Exception) {
        }

        return "Video"
    }

    private fun getVideoDuration(uri: Uri): Int {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)

            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            retriever.release()

            duration
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()

        } catch (_: Exception) {
            0
        }
    }

    private fun getVideoThumbnail(
        uri: Uri,
        timeMs: Int
    ) = try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)

        val bitmap = retriever.getFrameAtTime(
            timeMs.toLong() * 1000L,
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
            button.backgroundTintList =
                ColorStateList.valueOf(Color.rgb(35, 42, 48))
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

            setOnClickListener { showMediaImport() }
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
                toast("Templates arrive in a later milestone.")
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

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val REQUEST_VIDEO = 1001
        private const val REQUEST_AUDIO = 1002

        private const val SCREEN_HOME = 0
        private const val SCREEN_MEDIA = 1
        private const val SCREEN_EDITOR = 2

        private const val MIN_CLIP_MS = 250
        private const val MAX_HISTORY = 30
    }
}
