package com.jetbrains.kmpapp.data.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Проверка сравнения версий: формат YY.X[.Z][-dev.N|-beta.N|-rc.N|-contrib.N].
 * Ломается здесь → ломается автообновление у всех пользователей.
 */
class VersionComparatorTest {

    private fun assertOrder(less: String, more: String) {
        assertTrue(VersionComparator.compare(less, more) < 0, "ожидалось $less < $more")
        assertTrue(VersionComparator.compare(more, less) > 0, "ожидалось $more > $less")
    }

    @Test
    fun stableOrdering() {
        assertOrder("26.9.1", "26.10")     // старый формат не "даунгрейдится"
        assertOrder("26.9", "26.10")
        assertOrder("26.10", "26.11")
        assertOrder("26.10", "26.10.1")    // патч выше минорного релиза
        assertOrder("v26.10", "26.10.1")   // префикс v не влияет
    }

    @Test
    fun prereleaseBelowStable() {
        assertOrder("26.10-dev.151", "26.10")
        assertOrder("26.10-beta.2", "26.10")
        assertOrder("26.10-rc.1", "26.10")
    }

    @Test
    fun prereleaseOrderingWithinLine() {
        assertOrder("26.10-dev.9", "26.10-dev.10")   // числовое, не лексикографическое
        assertOrder("26.10-dev.10", "26.10-dev.151")
        assertOrder("26.10-contrib.5", "26.10-contrib.6")
        assertOrder("26.10-dev.151", "26.10-dev.151.1")
    }

    @Test
    fun acrossLines() {
        assertOrder("26.9.1", "26.10-dev.151")
        assertOrder("26.10-dev.151", "26.11-dev.1")
        assertOrder("26.10.1", "26.11")
    }

    @Test
    fun equalityAndBuildMeta() {
        assertEquals(0, VersionComparator.compare("26.10", "26.10"))
        assertEquals(0, VersionComparator.compare("26.10", "v26.10"))
        assertEquals(0, VersionComparator.compare("26.10-dev.5", "26.10-dev.5+1633184412.a3f5c2d"))
        assertOrder("26.10-dev.151+1633184412.a3f5c2d", "26.10-dev.152")
    }

    @Test
    fun buildMetaIgnored() {
        // Защита от атак: метаданные после + не влияют на сравнение
        assertEquals(0, VersionComparator.compare("26.10+evil", "26.10"))
        assertEquals(0, VersionComparator.compare("26.10+malicious.payload", "26.10+legitimate"))
        assertTrue(VersionComparator.compare("26.10-dev.1+evil", "26.10") < 0,
            "prerelease всегда ниже stable, несмотря на метаданные")
    }
}
