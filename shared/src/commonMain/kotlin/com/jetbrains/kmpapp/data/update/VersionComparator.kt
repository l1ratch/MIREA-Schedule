package com.jetbrains.kmpapp.data.update

/**
 * SemVer-подобное сравнение версий формата YY.X[.Z][-dev.N | -beta.N | -rc.N | -contrib.N][+meta].
 *
 * Правила:
 *  - числовое ядро сравнивается по сегментам (26.10 > 26.9.1, 26.10 < 26.10.1);
 *  - при равном ядре релиз выше prerelease (26.10 > 26.10-dev.151);
 *  - канальная лестница prerelease: dev < contrib < beta < rc
 *    (алфавит даёт «beta < dev» — beta-обновление выглядело «микро-правкой»
 *    для dev-сборки);
 *  - внутри одного канала — по SemVer: числовой идентификатор ниже буквенного,
 *    номера сравниваются числово (dev.9 < dev.10).
 */
object VersionComparator {

    fun compare(rawA: String, rawB: String): Int {
        val a = parse(rawA)
        val b = parse(rawB)
        val maxLen = maxOf(a.core.size, b.core.size)
        for (i in 0 until maxLen) {
            val x = a.core.getOrElse(i) { 0 }
            val y = b.core.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return when {
            a.pre.isEmpty() && b.pre.isEmpty() -> 0
            a.pre.isEmpty() -> 1
            b.pre.isEmpty() -> -1
            else -> comparePre(a.pre, b.pre)
        }
    }

    private class Parsed(val core: List<Int>, val pre: List<String>)

    private fun parse(raw: String): Parsed {
        val s = raw.trim().trimStart('v', 'V').substringBefore('+')
        val basePart = s.substringBefore('-')
        val prePart = if (s.contains('-')) s.substringAfter('-', "") else ""
        val core = basePart.split('.').mapNotNull { it.toIntOrNull() }
        val pre = if (prePart.isEmpty()) emptyList() else prePart.split('.')
        return Parsed(core, pre)
    }

    /** dev=0 < contrib=1 < beta=2 < rc=3; null — не наш канал (сравнение по SemVer). */
    private fun channelRank(id: String?): Int? = when (id) {
        "dev" -> 0
        "contrib" -> 1
        "beta" -> 2
        "rc" -> 3
        else -> null
    }

    private fun comparePre(a: List<String>, b: List<String>): Int {
        val ra = channelRank(a.firstOrNull())
        val rb = channelRank(b.firstOrNull())
        if (ra != null && rb != null && ra != rb) return ra.compareTo(rb)
        val n = minOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a[i]
            val y = b[i]
            if (x == y) continue
            val xn = x.toIntOrNull()
            val yn = y.toIntOrNull()
            val c = when {
                xn != null && yn != null -> xn.compareTo(yn)
                xn != null -> -1 // числовой идентификатор ниже буквенного (SemVer)
                yn != null -> 1
                else -> x.compareTo(y)
            }
            if (c != 0) return c
        }
        return a.size.compareTo(b.size)
    }
}
