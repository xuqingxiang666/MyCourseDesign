package com.example.mycoursedesign.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [MarkdownFormatter] 的单元测试。
 *
 * 覆盖：单行包裹/取消、空选区插入、多行逐行包裹、列表增删、有序编号、
 * 标题级别设置与取消、选区在文首/文末、行首已有前缀等边界。
 */
class MarkdownFormatterTest {

    // ==================== 行内包裹 ====================

    @Test
    fun `bold wraps selection`() {
        val r = MarkdownFormatter.toggleBold("这是文字", 2, 4)
        assertEquals("这是**文字**", r.text)
        // 选区应仍然覆盖"文字"两个字符（偏移 2）
        assertEquals(4, r.selectionStart)
        assertEquals(6, r.selectionEnd)
    }

    @Test
    fun `bold again unwraps`() {
        val text = "这是**文字**"
        // 选区在 ** 之内
        val r = MarkdownFormatter.toggleBold(text, 4, 6)
        assertEquals("这是文字", r.text)
        assertEquals(2, r.selectionStart)
        assertEquals(4, r.selectionEnd)
    }

    @Test
    fun `empty selection inserts marker pair and puts cursor inside`() {
        val r = MarkdownFormatter.toggleBold("abc", 1, 1)
        assertEquals("a****bc", r.text)
        assertEquals(3, r.selectionStart)
        assertEquals(3, r.selectionEnd)
    }

    @Test
    fun `italic and strikethrough and code use their markers`() {
        assertEquals("*x*", MarkdownFormatter.toggleItalic("x", 0, 1).text)
        assertEquals("~~x~~", MarkdownFormatter.toggleStrikethrough("x", 0, 1).text)
        assertEquals("`x`", MarkdownFormatter.toggleInlineCode("x", 0, 1).text)
    }

    @Test
    fun `multiline selection wraps each line`() {
        val text = "第一行\n第二行"
        val r = MarkdownFormatter.toggleBold(text, 0, text.length)
        assertEquals("**第一行**\n**第二行**", r.text)
    }

    @Test
    fun `multiline unwrap removes markers from each line`() {
        val text = "**第一行**\n**第二行**"
        val r = MarkdownFormatter.toggleBold(text, 0, text.length)
        assertEquals("第一行\n第二行", r.text)
    }

    // ==================== 无序列表 ====================

    @Test
    fun `bullet adds prefix to each selected line`() {
        val text = "苹果\n香蕉"
        val r = MarkdownFormatter.toggleBullet(text, 0, text.length)
        assertEquals("- 苹果\n- 香蕉", r.text)
    }

    @Test
    fun `bullet removes prefix when all lines already have it`() {
        val text = "- 苹果\n- 香蕉"
        val r = MarkdownFormatter.toggleBullet(text, 0, text.length)
        assertEquals("苹果\n香蕉", r.text)
    }

    @Test
    fun `bullet skips blank lines`() {
        val text = "苹果\n\n香蕉"
        val r = MarkdownFormatter.toggleBullet(text, 0, text.length)
        assertEquals("- 苹果\n\n- 香蕉", r.text)
    }

    @Test
    fun `bullet on single line without selection`() {
        val text = "苹果"
        val r = MarkdownFormatter.toggleBullet(text, 1, 1)
        assertEquals("- 苹果", r.text)
    }

    // ==================== 有序列表 ====================

    @Test
    fun `ordered list numbers sequentially`() {
        val text = "一\n二\n三"
        val r = MarkdownFormatter.toggleOrdered(text, 0, text.length)
        assertEquals("1. 一\n2. 二\n3. 三", r.text)
    }

    @Test
    fun `ordered list removes numbers when all prefixed`() {
        val text = "1. 一\n2. 二"
        val r = MarkdownFormatter.toggleOrdered(text, 0, text.length)
        assertEquals("一\n二", r.text)
    }

    // ==================== 标题 ====================

    @Test
    fun `heading adds prefix`() {
        val r = MarkdownFormatter.toggleHeading("标题", 0, 2, 1)
        assertEquals("# 标题", r.text)
    }

    @Test
    fun `heading same level removes prefix`() {
        val r = MarkdownFormatter.toggleHeading("# 标题", 0, 4, 1)
        assertEquals("标题", r.text)
    }

    @Test
    fun `heading different level replaces prefix`() {
        val r = MarkdownFormatter.toggleHeading("# 标题", 0, 4, 2)
        assertEquals("## 标题", r.text)
    }

    @Test
    fun `heading level clamped to 1 through 6`() {
        assertEquals("# 标题", MarkdownFormatter.toggleHeading("标题", 0, 2, 0).text)
        assertEquals("###### 标题", MarkdownFormatter.toggleHeading("标题", 0, 2, 99).text)
    }

    // ==================== 边界 ====================

    @Test
    fun `selection at text start`() {
        val r = MarkdownFormatter.toggleBold("文字", 0, 2)
        assertEquals("**文字**", r.text)
        assertEquals(2, r.selectionStart)
        assertEquals(4, r.selectionEnd)
    }

    @Test
    fun `selection at text end`() {
        val text = "前缀文字"
        val r = MarkdownFormatter.toggleBold(text, 2, 4)
        assertEquals("前缀**文字**", r.text)
    }

    @Test
    fun `empty text produces marker pair only`() {
        val r = MarkdownFormatter.toggleBold("", 0, 0)
        assertEquals("****", r.text)
        assertEquals(2, r.selectionStart)
    }

    @Test
    fun `out of range selection is clamped`() {
        val r = MarkdownFormatter.toggleBold("abc", -5, 99)
        assertEquals("**abc**", r.text)
    }

    @Test
    fun `single line range detection`() {
        val ranges = MarkdownFormatter.lineRanges("aa\nbb\ncc", 4, 5)
        assertEquals(1, ranges.size)
        assertEquals(3, ranges[0].start)
        // 左闭右开：[3, 5) 即 "bb" 两个字符
        assertEquals(5, ranges[0].end)
    }
}
