package com.example.feature_sharecard.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [TextWrapper] 的单元测试。
 *
 * 用"每个字符宽 10、每行最多 50"的假测量函数来验证折行规则，
 * 这样不需要 Android 的 Paint 就能精确断言每一行的内容。
 */
class TextWrapperTest {

    /** 每个字符按 10 计宽。 */
    private val measure: (String) -> Float = { it.length * 10f }

    /** 每行最多 5 个字符。 */
    private val width = 50f

    // ==================== 基本折行 ====================

    @Test
    fun `short text stays on one line`() {
        val lines = TextWrapper.wrap("abc", width, measure)
        assertEquals(listOf("abc"), lines)
    }

    @Test
    fun `text is wrapped at max width`() {
        val lines = TextWrapper.wrap("abcdefghij", width, measure)
        assertEquals(listOf("abcde", "fghij"), lines)
    }

    @Test
    fun `wrapping does not lose characters`() {
        val source = "abcdefghijklmnopqrstuvwxyz"
        val lines = TextWrapper.wrap(source, width, measure)
        assertEquals("折行不能丢字", source, lines.joinToString(""))
    }

    @Test
    fun `chinese text wraps by character`() {
        val lines = TextWrapper.wrap("今天天气很好我出门散步", width, measure)
        assertEquals(listOf("今天天气很", "好我出门散", "步"), lines)
        assertEquals("今天天气很好我出门散步", lines.joinToString(""))
    }

    @Test
    fun `exactly full line is not wrapped early`() {
        val lines = TextWrapper.wrap("abcde", width, measure)
        assertEquals(listOf("abcde"), lines)
    }

    // ==================== 显式换行 ====================

    @Test
    fun `explicit newline is preserved`() {
        val lines = TextWrapper.wrap("abc\ndef", width, measure)
        assertEquals(listOf("abc", "def"), lines)
    }

    @Test
    fun `blank line in middle is kept`() {
        val lines = TextWrapper.wrap("abc\n\ndef", width, measure)
        assertEquals(listOf("abc", "", "def"), lines)
    }

    @Test
    fun `each paragraph wraps independently`() {
        val lines = TextWrapper.wrap("abcdefg\nhij", width, measure)
        assertEquals(listOf("abcde", "fg", "hij"), lines)
    }

    // ==================== 最大行数与省略号 ====================

    @Test
    fun `max lines truncates with ellipsis`() {
        val lines = TextWrapper.wrap("abcdefghij", width, measure, maxLines = 1)
        assertEquals(1, lines.size)
        assertTrue("末尾应有省略号: ${lines[0]}", lines[0].endsWith("…"))
        assertTrue("截断后不能超宽", measure(lines[0]) <= width)
    }

    @Test
    fun `no ellipsis when content fits`() {
        val lines = TextWrapper.wrap("abc", width, measure, maxLines = 3)
        assertEquals(listOf("abc"), lines)
        assertTrue(lines.none { it.endsWith("…") })
    }

    @Test
    fun `zero max lines returns empty`() {
        assertTrue(TextWrapper.wrap("abc", width, measure, maxLines = 0).isEmpty())
    }

    @Test
    fun `max lines respects limit with many paragraphs`() {
        val lines = TextWrapper.wrap("a\nb\nc\nd", width, measure, maxLines = 2)
        assertEquals(2, lines.size)
    }

    // ==================== 边界 ====================

    @Test
    fun `empty text yields single empty line`() {
        assertEquals(listOf(""), TextWrapper.wrap("", width, measure))
    }

    @Test
    fun `single character wider than max width still returns it`() {
        // 极端情况：一个字就超宽。不能死循环，也不能丢字
        val lines = TextWrapper.wrap("a", 5f, measure)
        assertEquals(listOf("a"), lines)
    }

    @Test
    fun `very long unbroken text breaks by character`() {
        val lines = TextWrapper.wrap("x".repeat(20), width, measure)
        assertEquals(4, lines.size)
        assertTrue(lines.all { it.length <= 5 })
    }
}
