package com.cutrim.app

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

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
                openVideoPicker()
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
                openVideoPicker()
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

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }

        startActivityForResult(intent, REQUEST_VIDEO)
    }

    @Deprecated("Used for V1 compatibility without extra dependencies")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VIDEO && resultCode == RESULT_OK) {
            if (data?.data != null) {
                Toast.makeText(
                    this,
                    "Video selected. Media Import screen arrives in V2.",
                    Toast.LENGTH_LONG
                ).show()
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
