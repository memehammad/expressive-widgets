// Minimal JVM stand-ins for the few androidx helpers graphics-shapes uses (generator only).
package androidx.collection

data class FloatFloatPair(val first: Float, val second: Float)

open class FloatList(val items: ArrayList<Float>) {
    val size get() = items.size
    val lastIndex get() = items.size - 1
    val indices get() = items.indices
    operator fun get(i: Int) = items[i]
    fun first() = items.first()
    fun last() = items.last()
    fun isEmpty() = items.isEmpty()
    fun isNotEmpty() = items.isNotEmpty()
    inline fun forEach(f: (Float) -> Unit) { for (x in items) f(x) }
    inline fun forEachIndexed(f: (Int, Float) -> Unit) { items.forEachIndexed(f) }
    fun indexOfFirst(p: (Float) -> Boolean) = items.indexOfFirst(p)
    fun binarySearch(element: Float, fromIndex: Int = 0, toIndex: Int = size): Int =
        items.binarySearch(element, fromIndex, toIndex)
    fun joinToString(separator: CharSequence = ", "): String = items.joinToString(separator)
    override fun toString() = items.toString()
}

class MutableFloatList(capacity: Int = 16) : FloatList(ArrayList(capacity)) {
    fun add(x: Float): Boolean = items.add(x)
    operator fun plusAssign(x: Float) { items.add(x) }
    operator fun set(i: Int, x: Float): Float { val o = items[i]; items[i] = x; return o }
    fun clear() = items.clear()
}

fun mutableFloatListOf(vararg xs: Float) = MutableFloatList(xs.size).also { l -> xs.forEach { l.add(it) } }
