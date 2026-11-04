package dev.memehammad.widgets

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Opens what a widget list row points at (e.g. a calendar event). Widget list rows need a
 * mutable PendingIntent, which Android only allows for explicit intents, so rows target this
 * activity and it forwards the view intent.
 */
class OpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let { uri ->
            runCatching {
                startActivity(
                    Intent(Intent.ACTION_VIEW, uri).putExtras(intent.extras ?: Bundle())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
        finish()
    }
}
