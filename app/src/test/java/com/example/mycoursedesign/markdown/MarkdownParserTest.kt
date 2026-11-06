package com.example.mycoursedesign.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [MarkdownParser] 的单元测试。
 *
 * 【为什么能跑在 JVM 上】
 * 解析器是纯 Kotlin（不碰 Android 的 Spannable），所以 `gradlew :app:testDebugUnitTest`
 * 就能跑，不需要模拟器。渲染器 [MarkdownRenderer] 才依赖 Android。
 *
 * 【覆盖范围】
 * 块级：标题 1~6 级、段落合并、空行分段、无序/有序列表、引用、代码块、分割线
 * 行内：粗体、斜体、粗斜体、删除线、行内代码、链接
 * 边界：空字符串、未闭合标记、代码块内不解析行内标记、嵌套样式
 */
class MarkdownParserTest {

    // ==================== 块级：标题 ====================

    @Test
    fun `heading levels 1 to 6`() {
        for (level in 1..6) {
            val md = "#".repeat(level) + " 标题"
            val blocks = MarkdownParser.parse(md)
            assertEquals(1, blocks.size)
            val heading = blocks[0] as MdBlock.Heading
            assertEquals(level, heading.level)
            assertEquals("标题", heading.inlines.joinToString("") { it.text })
        }
    }

    @Test
    fun `seven hashes is not a heading`() {
        val blocks = MarkdownParser.parse("####### 太多井号")
        assertTrue(blocks[0] is MdBlock.Paragraph)
    }

    @Test
    fun `heading without space is paragraph`() {
        val blocks = MarkdownParser.parse("#没有空格")
        assertTrue(blocks[0] is MdBlock.Paragraph)
    }

    // ==================== 块级：段落 ====================

    @Test
    fun `consecutive lines merge into one paragraph`() {
        val blocks = MarkdownParser.parse("第一行\n第二行")
        assertEquals(1, blocks.size)
        val p = blocks[0] as MdBlock.Paragraph
        assertEquals("第一行 第二行", p.inlines.joinToString("") { it.text })
    }

    @Test
    fun `blank line splits paragraphs`() {
        val blocks = MarkdownParser.parse("段落一\n\n段落二")
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MdBlock.Paragraph)
        assertTrue(blocks[1] is MdBlock.Paragraph)
    }

    // ==================== 块级：列表 / 引用 / 分割线 ====================

    @Test
    fun `bullet list items`() {
        val blocks = MarkdownParser.parse("- 苹果\n- 香蕉\n* 橘子")
        assertEquals(3, blocks.size)
        assertTrue(blocks.all { it is MdBlock.Bullet })
        assertEquals("苹果", (blocks[0] as MdBlock.Bullet).inlines.joinToString("") { it.text })
    }

    @Test
    fun `ordered list keeps numbers`() {
        val blocks = MarkdownParser.parse("1. 第一\n2. 第二")
        assertEquals(2, blocks.size)
        assertEquals(1, (blocks[0] as MdBlock.Ordered).number)
        assertEquals(2, (blocks[1] as MdBlock.Ordered).number)
    }

    @Test
    fun `quote block`() {
        val blocks = MarkdownParser.parse("> 引用内容")
        assertEquals(1, blocks.size)
        val q = blocks[0] as MdBlock.Quote
        assertEquals("引用内容", q.inlines.joinToString("") { it.text })
    }

    @Test
    fun `divider variants`() {
        listOf("---", "***", "___", "-----").forEach { md ->
            val blocks = MarkdownParser.parse(md)
            assertEquals("输入 $md 应解析为分割线", MdBlock.Divider, blocks[0])
        }
    }

    // ==================== 块级：代码块 ====================

    @Test
    fun `fenced code block keeps raw content`() {
        val md = "```\nval a = 1\nval b = 2\n```"
        val blocks = MarkdownParser.parse(md)
        assertEquals(1, blocks.size)
        val code = blocks[0] as MdBlock.CodeBlock
        assertEquals("val a = 1\nval b = 2", code.code)
    }

    @Test
    fun `code block does not parse inline markers`() {
        val md = "```\n**不是粗体**\n```"
        val blocks = MarkdownParser.parse(md)
        val code = blocks[0] as MdBlock.CodeBlock
        assertEquals("**不是粗体**", code.code)
    }

    @Test
    fun `unclosed code block is kept so user can keep typing`() {
        val md = "```\n还没写完"
        val blocks = MarkdownParser.parse(md)
        val code = blocks[0] as MdBlock.CodeBlock
        assertEquals("还没写完", code.code)
    }

    // ==================== 行内：样式 ====================

    @Test
    fun `bold inline`() {
        val inlines = MarkdownParser.parseInline("这是**粗体**结束")
        assertEquals(3, inlines.size)
        assertEquals("这是", inlines[0].text)
        assertEquals("粗体", inlines[1].text)
        assertTrue(inlines[1].styles.contains(InlineStyle.BOLD))
        assertEquals("结束", inlines[2].text)
    }

    @Test
    fun `italic inline`() {
        val inlines = MarkdownParser.parseInline("*斜体*")
        assertEquals("斜体", inlines[0].text)
        assertTrue(inlines[0].styles.contains(InlineStyle.ITALIC))
    }

    @Test
    fun `bold italic triple star`() {
        val inlines = MarkdownParser.parseInline("***又粗又斜***")
        assertEquals("又粗又斜", inlines[0].text)
        assertTrue(inlines[0].styles.contains(InlineStyle.BOLD))
        assertTrue(inlines[0].styles.contains(InlineStyle.ITALIC))
    }

    @Test
    fun `strikethrough inline`() {
        val inlines = MarkdownParser.parseInline("~~删掉~~")
        assertEquals("删掉", inlines[0].text)
        assertTrue(inlines[0].styles.contains(InlineStyle.STRIKETHROUGH))
    }

    @Test
    fun `inline code`() {
        val inlines = MarkdownParser.parseInline("用 `adb devices` 查看")
        assertEquals(3, inlines.size)
        assertEquals("adb devices", inlines[1].text)
        assertTrue(inlines[1].styles.contains(InlineStyle.CODE))
    }

    @Test
    fun `inline code content is not parsed`() {
        val inlines = MarkdownParser.parseInline("`**不该是粗体**`")
        assertEquals(1, inlines.size)
        assertEquals("**不该是粗体**", inlines[0].text)
        assertTrue(inlines[0].styles.contains(InlineStyle.CODE))
    }

    @Test
    fun `link with url`() {
        val inlines = MarkdownParser.parseInline("见[文档](https://example.com)说明")
        val link = inlines.first { it.linkUrl != null }
        assertEquals("文档", link.text)
        assertEquals("https://example.com", link.linkUrl)
    }

    @Test
    fun `nested bold containing italic`() {
        val inlines = MarkdownParser.parseInline("**粗体里有*斜体*部分**")
        val italic = inlines.first { it.styles.contains(InlineStyle.ITALIC) }
        // 外层粗体 + 内层斜体，两个样式都要保留
        assertTrue(italic.styles.contains(InlineStyle.BOLD))
        assertEquals("斜体", italic.text)
    }

    // ==================== 边界情况 ====================

    @Test
    fun `empty input yields no blocks`() {
        assertTrue(MarkdownParser.parse("").isEmpty())
    }

    @Test
    fun `unclosed bold is treated as plain text`() {
        val inlines = MarkdownParser.parseInline("**没有闭合")
        val joined = inlines.joinToString("") { it.text }
        assertEquals("**没有闭合", joined)
        assertTrue(inlines.all { it.styles.isEmpty() })
    }

    @Test
    fun `unclosed link is treated as plain text`() {
        val inlines = MarkdownParser.parseInline("[没有链接]")
        assertEquals("[没有链接]", inlines.joinToString("") { it.text })
        assertNull(inlines.firstOrNull { it.linkUrl != null })
    }

    @Test
    fun `plain text has no styles`() {
        val inlines = MarkdownParser.parseInline("普通文字 123 ABC")
        assertEquals(1, inlines.size)
        assertEquals("普通文字 123 ABC", inlines[0].text)
        assertTrue(inlines[0].styles.isEmpty())
        assertNull(inlines[0].linkUrl)
    }

    // ==================== 纯文本提取（列表预览用）====================

    @Test
    fun `toPlainText strips markers`() {
        val md = "# 标题\n\n这是**粗体**和`代码`"
        val plain = MarkdownParser.toPlainText(md)
        assertEquals("标题\n这是粗体和代码", plain)
    }

    @Test
    fun `toPlainText adds list prefixes`() {
        val plain = MarkdownParser.toPlainText("- 项目一\n- 项目二")
        assertEquals("• 项目一\n• 项目二", plain)
    }

    // ==================== 综合场景 ====================

    @Test
    fun `realistic note renders all block types`() {
        val md = """
            # 会议纪要

            讨论**项目代号**与`交付日期`。

            ## 待办
            - 完成 Markdown 渲染
            - 补测试

            > 下周一前完成

            ---

            ```
            val x = 1
            ```
        """.trimIndent()

        val blocks = MarkdownParser.parse(md)
        assertTrue("应包含标题", blocks.any { it is MdBlock.Heading })
        assertTrue("应包含段落", blocks.any { it is MdBlock.Paragraph })
        assertTrue("应包含无序列表", blocks.any { it is MdBlock.Bullet })
        assertTrue("应包含引用", blocks.any { it is MdBlock.Quote })
        assertTrue("应包含分割线", blocks.contains(MdBlock.Divider))
        assertTrue("应包含代码块", blocks.any { it is MdBlock.CodeBlock })
    }
}
