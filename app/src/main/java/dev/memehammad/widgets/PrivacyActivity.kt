package dev.memehammad.widgets

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** Explains what health data is read and why (shown by Health Connect). */
class PrivacyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                setText(R.string.health_rationale)
                textSize = 18f
                setPadding(64, 160, 64, 64)
                setTextColor(getColor(R.color.w_text))
            },
        )
    }
}
