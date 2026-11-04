// Build-time generator: the 35 Material 3 Expressive shapes, ported verbatim from
// androidx.compose.material3.MaterialShapes (Apache 2.0) and computed with Google's
// androidx.graphics.shapes library, written out as path data (0..100 viewport).
package gen

import androidx.collection.FloatFloatPair
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.PointTransformer
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Offset(val x: Float, val y: Float) {
    operator fun minus(o: Offset) = Offset(x - o.x, y - o.y)
    operator fun plus(o: Offset) = Offset(x + o.x, y + o.y)
    operator fun times(f: Float) = Offset(x * f, y * f)
    fun getDistance() = sqrt(x * x + y * y)
    companion object { val Zero = Offset(0f, 0f) }
}

fun rotate(deg: Float) = PointTransformer { x, y ->
    val a = deg / 180f * PI.toFloat()
    FloatFloatPair(x * cos(a) - y * sin(a), x * sin(a) + y * cos(a))
}

fun scale(sx: Float, sy: Float) = PointTransformer { x, y -> FloatFloatPair(x * sx, y * sy) }

object M {
    private val cornerRound15 = CornerRounding(radius = .15f)
    private val cornerRound20 = CornerRounding(radius = .2f)
    private val cornerRound30 = CornerRounding(radius = .3f)
    private val cornerRound50 = CornerRounding(radius = .5f)
    private val cornerRound100 = CornerRounding(radius = 1f)
    private val rotateNeg45 = rotate(-45f)
    private val rotateNeg90 = rotate(-90f)
    private val rotateNeg135 = rotate(-135f)

    fun circle(numVertices: Int = 10) = RoundedPolygon.circle(numVertices = numVertices)
    fun square() = RoundedPolygon.rectangle(width = 1f, height = 1f, rounding = cornerRound30)
    fun slanted() = customPolygon(listOf(
        P(Offset(0.926f, 0.970f), CornerRounding(0.189f, 0.811f)),
        P(Offset(-0.021f, 0.967f), CornerRounding(0.187f, 0.057f)),
    ), 2)
    fun arch() = RoundedPolygon(numVertices = 4, perVertexRounding = listOf(cornerRound100, cornerRound100, cornerRound20, cornerRound20))
        .transformed(rotateNeg135)
    fun fan() = customPolygon(listOf(
        P(Offset(1.004f, 1.000f), CornerRounding(0.148f, 0.417f)),
        P(Offset(0.000f, 1.000f), CornerRounding(0.151f)),
        P(Offset(0.000f, -0.003f), CornerRounding(0.148f)),
        P(Offset(0.978f, 0.020f), CornerRounding(0.803f)),
    ), 1)
    fun arrow() = customPolygon(listOf(
        P(Offset(0.500f, 0.892f), CornerRounding(0.313f)),
        P(Offset(-0.216f, 1.050f), CornerRounding(0.207f)),
        P(Offset(0.499f, -0.160f), CornerRounding(0.215f, 1.000f)),
        P(Offset(1.225f, 1.060f), CornerRounding(0.211f)),
    ), 1)
    fun semiCircle() = RoundedPolygon.rectangle(width = 1.6f, height = 1f,
        perVertexRounding = listOf(cornerRound20, cornerRound20, cornerRound100, cornerRound100))
    fun oval() = RoundedPolygon.circle().transformed(scale(1f, 0.64f)).transformed(rotateNeg45)
    fun pill() = customPolygon(listOf(
        P(Offset(0.961f, 0.039f), CornerRounding(0.426f)),
        P(Offset(1.001f, 0.428f)),
        P(Offset(1.000f, 0.609f), CornerRounding(1.000f)),
    ), reps = 2, mirroring = true)
    fun triangle() = RoundedPolygon(numVertices = 3, rounding = cornerRound20).transformed(rotateNeg90)
    fun diamond() = customPolygon(listOf(
        P(Offset(0.500f, 1.096f), CornerRounding(0.151f, 0.524f)),
        P(Offset(0.040f, 0.500f), CornerRounding(0.159f)),
    ), 2)
    fun clamShell() = customPolygon(listOf(
        P(Offset(0.171f, 0.841f), CornerRounding(0.159f)),
        P(Offset(-0.020f, 0.500f), CornerRounding(0.140f)),
        P(Offset(0.170f, 0.159f), CornerRounding(0.159f)),
    ), 2)
    fun pentagon() = customPolygon(listOf(
        P(Offset(0.500f, -0.009f), CornerRounding(0.172f)),
        P(Offset(1.030f, 0.365f), CornerRounding(0.164f)),
        P(Offset(0.828f, 0.970f), CornerRounding(0.169f)),
    ), reps = 1, mirroring = true)
    fun gem() = customPolygon(listOf(
        P(Offset(0.499f, 1.023f), CornerRounding(0.241f, 0.778f)),
        P(Offset(-0.005f, 0.792f), CornerRounding(0.208f)),
        P(Offset(0.073f, 0.258f), CornerRounding(0.228f)),
        P(Offset(0.433f, -0.000f), CornerRounding(0.491f)),
    ), 1, mirroring = true)
    fun sunny() = RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = .8f, rounding = cornerRound15)
    fun verySunny() = customPolygon(listOf(
        P(Offset(0.500f, 1.080f), CornerRounding(0.085f)),
        P(Offset(0.358f, 0.843f), CornerRounding(0.085f)),
    ), 8)
    fun cookie4() = customPolygon(listOf(
        P(Offset(1.237f, 1.236f), CornerRounding(0.258f)),
        P(Offset(0.500f, 0.918f), CornerRounding(0.233f)),
    ), 4)
    fun cookie6() = customPolygon(listOf(
        P(Offset(0.723f, 0.884f), CornerRounding(0.394f)),
        P(Offset(0.500f, 1.099f), CornerRounding(0.398f)),
    ), 6)
    fun cookie7() = RoundedPolygon.star(numVerticesPerRadius = 7, innerRadius = .75f, rounding = cornerRound50).transformed(rotateNeg90)
    fun cookie9() = RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = .8f, rounding = cornerRound50).transformed(rotateNeg90)
    fun cookie12() = RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = .8f, rounding = cornerRound50).transformed(rotateNeg90)
    fun ghostish() = customPolygon(listOf(
        P(Offset(0.500f, 0f), CornerRounding(1.000f)),
        P(Offset(1f, 0f), CornerRounding(1.000f)),
        P(Offset(1f, 1.140f), CornerRounding(0.254f, 0.106f)),
        P(Offset(0.575f, 0.906f), CornerRounding(0.253f)),
    ), reps = 1, mirroring = true)
    fun clover4() = customPolygon(listOf(
        P(Offset(0.500f, 0.074f)),
        P(Offset(0.725f, -0.099f), CornerRounding(0.476f)),
    ), reps = 4, mirroring = true)
    fun clover8() = customPolygon(listOf(
        P(Offset(0.500f, 0.036f)),
        P(Offset(0.758f, -0.101f), CornerRounding(0.209f)),
    ), reps = 8)
    fun burst() = customPolygon(listOf(
        P(Offset(0.500f, -0.006f), CornerRounding(0.006f)),
        P(Offset(0.592f, 0.158f), CornerRounding(0.006f)),
    ), reps = 12)
    fun softBurst() = customPolygon(listOf(
        P(Offset(0.193f, 0.277f), CornerRounding(0.053f)),
        P(Offset(0.176f, 0.055f), CornerRounding(0.053f)),
    ), reps = 10)
    fun boom() = customPolygon(listOf(
        P(Offset(0.457f, 0.296f), CornerRounding(0.007f)),
        P(Offset(0.500f, -0.051f), CornerRounding(0.007f)),
    ), reps = 15)
    fun softBoom() = customPolygon(listOf(
        P(Offset(0.733f, 0.454f)),
        P(Offset(0.839f, 0.437f), CornerRounding(0.532f)),
        P(Offset(0.949f, 0.449f), CornerRounding(0.439f, 1.000f)),
        P(Offset(0.998f, 0.478f), CornerRounding(0.174f)),
    ), reps = 16, mirroring = true)
    fun flower() = customPolygon(listOf(
        P(Offset(0.370f, 0.187f)),
        P(Offset(0.416f, 0.049f), CornerRounding(0.381f)),
        P(Offset(0.479f, 0.001f), CornerRounding(0.095f)),
    ), reps = 8, mirroring = true)
    fun puffy() = customPolygon(listOf(
        P(Offset(0.500f, 0.053f)),
        P(Offset(0.545f, -0.040f), CornerRounding(0.405f)),
        P(Offset(0.670f, -0.035f), CornerRounding(0.426f)),
        P(Offset(0.717f, 0.066f), CornerRounding(0.574f)),
        P(Offset(0.722f, 0.128f)),
        P(Offset(0.777f, 0.002f), CornerRounding(0.360f)),
        P(Offset(0.914f, 0.149f), CornerRounding(0.660f)),
        P(Offset(0.926f, 0.289f), CornerRounding(0.660f)),
        P(Offset(0.881f, 0.346f)),
        P(Offset(0.940f, 0.344f), CornerRounding(0.126f)),
        P(Offset(1.003f, 0.437f), CornerRounding(0.255f)),
    ), reps = 2, mirroring = true).transformed(scale(1f, 0.742f))
    fun puffyDiamond() = customPolygon(listOf(
        P(Offset(0.870f, 0.130f), CornerRounding(0.146f)),
        P(Offset(0.818f, 0.357f)),
        P(Offset(1.000f, 0.332f), CornerRounding(0.853f)),
    ), reps = 4, mirroring = true)
    fun pixelCircle() = customPolygon(listOf(
        P(Offset(0.500f, 0.000f)), P(Offset(0.704f, 0.000f)), P(Offset(0.704f, 0.065f)),
        P(Offset(0.843f, 0.065f)), P(Offset(0.843f, 0.148f)), P(Offset(0.926f, 0.148f)),
        P(Offset(0.926f, 0.296f)), P(Offset(1.000f, 0.296f)),
    ), reps = 2, mirroring = true)
    fun pixelTriangle() = customPolygon(listOf(
        P(Offset(0.110f, 0.500f)), P(Offset(0.113f, 0.000f)), P(Offset(0.287f, 0.000f)),
        P(Offset(0.287f, 0.087f)), P(Offset(0.421f, 0.087f)), P(Offset(0.421f, 0.170f)),
        P(Offset(0.560f, 0.170f)), P(Offset(0.560f, 0.265f)), P(Offset(0.674f, 0.265f)),
        P(Offset(0.675f, 0.344f)), P(Offset(0.789f, 0.344f)), P(Offset(0.789f, 0.439f)),
        P(Offset(0.888f, 0.439f)),
    ), reps = 1, mirroring = true)
    fun bun() = customPolygon(listOf(
        P(Offset(0.796f, 0.500f)),
        P(Offset(0.853f, 0.518f), CornerRounding(1f)),
        P(Offset(0.992f, 0.631f), CornerRounding(1f)),
        P(Offset(0.968f, 1.000f), CornerRounding(1f)),
    ), reps = 2, mirroring = true)
    fun heart() = customPolygon(listOf(
        P(Offset(0.500f, 0.268f), CornerRounding(0.016f)),
        P(Offset(0.792f, -0.066f), CornerRounding(0.958f)),
        P(Offset(1.064f, 0.276f), CornerRounding(1.000f)),
        P(Offset(0.501f, 0.946f), CornerRounding(0.129f)),
    ), reps = 1, mirroring = true)

    data class P(val o: Offset, val r: CornerRounding = CornerRounding.Unrounded)

    private fun doRepeat(points: List<P>, reps: Int, center: Offset, mirroring: Boolean) =
        if (mirroring) {
            buildList {
                val angles = points.map { (it.o - center).angleDegrees() }
                val distances = points.map { (it.o - center).getDistance() }
                val actualReps = reps * 2
                val sectionAngle = 360f / actualReps
                repeat(actualReps) {
                    points.indices.forEach { index ->
                        val i = if (it % 2 == 0) index else points.lastIndex - index
                        if (i > 0 || it % 2 == 0) {
                            val a = (sectionAngle * it +
                                if (it % 2 == 0) angles[i] else sectionAngle - angles[i] + 2 * angles[0]).toRadians()
                            val finalPoint = Offset(cos(a), sin(a)) * distances[i] + center
                            add(P(finalPoint, points[i].r))
                        }
                    }
                }
            }
        } else {
            points.size.let { np ->
                (0 until np * reps).map {
                    val point = points[it % np].o.rotateDegrees((it / np) * 360f / reps, center)
                    P(point, points[it % np].r)
                }
            }
        }

    private fun Offset.rotateDegrees(angle: Float, center: Offset = Offset.Zero) =
        angle.toRadians().let { a ->
            val off = this - center
            Offset(off.x * cos(a) - off.y * sin(a), off.x * sin(a) + off.y * cos(a)) + center
        }

    private fun Float.toRadians(): Float = this / 360f * 2 * PI.toFloat()
    private fun Offset.angleDegrees() = atan2(y, x) * 180f / PI.toFloat()

    private fun customPolygon(pnr: List<P>, reps: Int, center: Offset = Offset(0.5f, 0.5f), mirroring: Boolean = false): RoundedPolygon {
        val actualPoints = doRepeat(pnr, reps, center, mirroring)
        return RoundedPolygon(
            vertices = FloatArray(actualPoints.size * 2) { ix -> actualPoints[ix / 2].o.let { if (ix % 2 == 0) it.x else it.y } },
            perVertexRounding = buildList { for (p in actualPoints) add(p.r) },
            centerX = center.x,
            centerY = center.y,
        )
    }

    val all: List<Pair<String, () -> RoundedPolygon>> = listOf(
        "circle" to { circle() }, "square" to ::square, "slanted" to ::slanted, "arch" to ::arch, "fan" to ::fan,
        "arrow" to ::arrow, "semicircle" to ::semiCircle, "oval" to ::oval, "pill" to ::pill, "triangle" to ::triangle,
        "diamond" to ::diamond, "clamshell" to ::clamShell, "pentagon" to ::pentagon, "gem" to ::gem,
        "sunny" to ::sunny, "verysunny" to ::verySunny, "cookie4" to ::cookie4, "cookie6" to ::cookie6,
        "cookie7" to ::cookie7, "cookie9" to ::cookie9, "cookie12" to ::cookie12, "ghostish" to ::ghostish,
        "clover4" to ::clover4, "clover8" to ::clover8, "burst" to ::burst, "softburst" to ::softBurst,
        "boom" to ::boom, "softboom" to ::softBoom, "flower" to ::flower, "puffy" to ::puffy,
        "puffydiamond" to ::puffyDiamond, "pixelcircle" to ::pixelCircle, "pixeltriangle" to ::pixelTriangle,
        "bun" to ::bun, "heart" to ::heart,
    )
}

fun f(v: Float) = "%.2f".format(v * 100)

fun pathOf(cubics: List<Cubic>): String {
    val sb = StringBuilder()
    val c0 = cubics.first()
    sb.append("M").append(f(c0.anchor0X)).append(",").append(f(c0.anchor0Y))
    for (c in cubics) {
        sb.append(" C").append(f(c.control0X)).append(",").append(f(c.control0Y)).append(" ")
            .append(f(c.control1X)).append(",").append(f(c.control1Y)).append(" ")
            .append(f(c.anchor1X)).append(",").append(f(c.anchor1Y))
    }
    return sb.append("Z").toString()
}

fun esc(s: String) = s.replace("\"", "\\\"")

/** args: outFile [morph pairs like "circle>cookie9" ...] */
fun main(args: Array<String>) {
    val polys = M.all.associate { (n, fn) -> n to fn().normalized() }
    val sb = StringBuilder("{\n  \"shapes\": {\n")
    sb.append(polys.entries.joinToString(",\n") { (n, p) -> "    \"$n\": \"${esc(pathOf(p.cubics))}\"" })
    sb.append("\n  },\n  \"morphs\": {\n")
    val morphs = args.drop(1).mapNotNull { pair ->
        val (a, b) = pair.split(">")
        val pa = polys[a] ?: return@mapNotNull null
        val pb = polys[b] ?: return@mapNotNull null
        val m = Morph(pa, pb)
        "    \"$pair\": [\"${pathOf(m.asCubics(0f))}\", \"${pathOf(m.asCubics(1f))}\"]"
    }
    sb.append(morphs.joinToString(",\n")).append("\n  }\n}\n")
    File(args[0]).writeText(sb.toString())
    println("${polys.size} shapes, ${morphs.size} morphs")
}
