package dev.memehammad.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.SizeF
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Draws widgets in different colour settings to PNGs (pulled by the "renders" workflow). */
@RunWith(AndroidJUnit4::class)
class RenderTest {
    @Test
    fun renderWidgets() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val ctx = inst.targetContext
        Palette.app = ctx.applicationContext
        val out = File(ctx.getExternalFilesDir(null), "renders").apply { mkdirs() }
        val variants = listOf(
            "primary_soft" to Cfg().with("role", "primary").with("fill", "tonal"),
            "primary_bold" to Cfg().with("role", "primary").with("fill", "bold"),
            "tertiary_soft" to Cfg().with("role", "tertiary").with("fill", "tonal"),
            "tertiary_bold" to Cfg().with("role", "tertiary").with("fill", "bold"),
            "custom_soft" to Cfg().with("role", "custom").with("fill", "tonal").with("cfill_l", "a3:500").with("cfill_d", "a3:500"),
            "card_icons" to Cfg().with("role", "icons").with("card", true).with("cardColor", "surface"),
        )
        val sizes = listOf(SizeF(160f, 160f), SizeF(320f, 140f))
        inst.runOnMainSync {
            for (kind in Widgets.kinds) {
                for ((name, extra) in variants) {
                    var cfg = kind.defaults()
                    for (k in extra.json.keys()) cfg = cfg.with(k, extra.json.get(k))
                    for (size in sizes) {
                        val file = File(out, "${kind.id}_${name}_${size.width.toInt()}x${size.height.toInt()}.png")
                        try {
                            Sizes.forced = size
                            val rv = kind.render(ctx, 0, cfg)
                            Sizes.forced = null
                            val parent = FrameLayout(ctx)
                            val v = rv.apply(ctx, parent)
                            val d = ctx.resources.displayMetrics.density
                            val w = (size.width * d).toInt()
                            val h = (size.height * d).toInt()
                            v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                            v.layout(0, 0, w, h)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            canvas.drawColor(Color.rgb(60, 80, 90))
                            v.draw(canvas)
                            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        } catch (e: Throwable) {
                            Sizes.forced = null
                            File(out, file.name + ".txt").writeText(e.stackTraceToString())
                        }
                    }
                }
            }
        }
    }
}
