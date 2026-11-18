package com.example.mycoursedesign.markdown

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Markdown 解析器的轻量压力测试。
 *
 * 用一份约 5 万字的 Markdown 文档验证解析性能 —— 用户粘贴长文档时
 * 预览不能卡住界面。
 */
class MarkdownStressTest {

    private fun bigDocument(repeats: Int): String = buildString {
        repeat(repeats) { i ->
            appendLine("# 第 $i 节")
            appendLine()
            appendLine("这是**粗体**与*斜体*以及 `代码` 的测试段落，用来模拟真实笔记内容。")
            appendLine("- 列表项 A")
            appendLine("- 列表项 B")
            appendLine("> 引用一行")
            appendLine()
        }
    }

    @Test
    fun `parse 500 sections quickly`() {
        val doc = bigDocument(500)
        println("[STRESS] 文档长度 = ${doc.length} 字符")

        val start = System.nanoTime()
        val blocks = MarkdownParser.parse(doc)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        println("[STRESS] MarkdownParser.parse 500 节耗时 = ${elapsedMs}ms（块数 ${blocks.size}）")
        assertTrue("块数应远大于 500，实际 ${blocks.size}", blocks.size > 500)
        assertTrue("5 万字文档解析应在 2000ms 内完成，实际 ${elapsedMs}ms", elapsedMs < 2000)
    }

    @Test
    fun `toPlainText on big document completes quickly`() {
        val doc = bigDocument(300)
        val start = System.nanoTime()
        val plain = MarkdownParser.toPlainText(doc)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        println("[STRESS] toPlainText 300 节耗时 = ${elapsedMs}ms（输出 ${plain.length} 字符）")
        assertTrue("纯文本提取应在 2000ms 内完成，实际 ${elapsedMs}ms", elapsedMs < 2000)
    }

    @Test
    fun `formatter handles long selection`() {
        val text = "内容 内容 内容\n".repeat(200)
        val start = System.nanoTime()
        repeat(200) {
            MarkdownFormatter.toggleBold(text, 0, text.length)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        println("[STRESS] 全选加粗 200 次耗时 = ${elapsedMs}ms")
        assertTrue("整篇格式化应在 3000ms 内完成，实际 ${elapsedMs}ms", elapsedMs < 3000)
    }
}
