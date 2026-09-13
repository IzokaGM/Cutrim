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
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

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
    private var onMediaScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        showHome()
    }

    private fun showHome() {
        onMediaScreen = false

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
        onMediaScreen = true

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
                    Toast.makeText(
                        this@MainActivity,
                        "${selectedVideos.size} video(s) ready. Editor arrives in V3.",
                        Toast.LENGTH_LONG
                    ).show()
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

    @Deprecated("Used for V2 without extra dependencies")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_VIDEO || resultCode != RESULT_OK || data == null) {
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

        if (!onMediaScreen) {
            showMediaImport()
        } else {
            refreshSelectedVideos()
        }
    }

    @Deprecated("Handled for current Activity UI")
    override fun onBackPressed() {
        if (onMediaScreen) {
            showHome()
        } else {
            super.onBackPressed()
        }
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
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
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

    private fun sectionTitle(text: String, topMargin: Int): View {
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

    private fun roundedBackground(color: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
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
    }
}
