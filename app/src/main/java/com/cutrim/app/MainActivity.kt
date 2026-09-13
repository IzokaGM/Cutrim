package com.cutrim.app

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var selectedVideoText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fun dp(value: Int): Int {
            return (value * resources.displayMetrics.density).toInt()
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL

            setPadding(
                dp(24),
                dp(72),
                dp(24),
                dp(24)
            )

            setBackgroundColor(
                Color.rgb(9, 14, 18)
            )
        }

        val logo = TextView(this).apply {
            text = "CUTRIM"
            textSize = 34f

            setTextColor(
                Color.rgb(34, 230, 199)
            )

            setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Create. Edit. Share."
            textSize = 16f

            setTextColor(
                Color.rgb(190, 198, 204)
            )

            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

        val newProjectButton = Button(this).apply {
            text = "New Project"
            textSize = 17f
            isAllCaps = false

            setTextColor(Color.BLACK)

            backgroundTintList =
                ColorStateList.valueOf(
                    Color.rgb(34, 230, 199)
                )

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                topMargin = dp(42)
            }

            setOnClickListener {
                val intent =
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {

                        addCategory(
                            Intent.CATEGORY_OPENABLE
                        )

                        type = "video/*"
                    }

                startActivityForResult(
                    intent,
                    REQUEST_VIDEO
                )
            }
        }

        selectedVideoText = TextView(this).apply {
            text = "No video selected"
            textSize = 14f

            setTextColor(
                Color.rgb(145, 155, 162)
            )

            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(24)
            }
        }

        root.addView(logo)
        root.addView(subtitle)
        root.addView(newProjectButton)
        root.addView(selectedVideoText)

        setContentView(root)
    }

    @Deprecated("Deprecated in Android API but suitable for bootstrap")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == REQUEST_VIDEO &&
            resultCode == RESULT_OK
        ) {
            val uri = data?.data

            selectedVideoText.text =
                if (uri != null) {
                    "Video selected ✓"
                } else {
                    "Unable to read video"
                }
        }
    }

    companion object {
        private const val REQUEST_VIDEO = 1001
    }
}
