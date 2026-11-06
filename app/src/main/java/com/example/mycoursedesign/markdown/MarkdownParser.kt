package com.example.mycoursedesign.markdown

/**
 * Markdown 解析器：把纯文本解析成 [MdBlock] 列表。
 *
 * 【支持范围（课设够用、且不引入第三方库）】
 * 块级：`#`~`######` 标题、`- * +` 无序列表、`1.` 有序列表、`>` 引用、
 *       ``` 围栏代码块、`---` 分割线、普通段落
 * 行内：`**粗体**`、`*斜体*`、`***粗斜体***`、`~~删除线~~`、`` `代码` ``、
 *       `[文字](链接)`
 *
 * 【为什么不用第三方 Markdown 库】
 * 1. 课设明确要求"防抄袭的定制功能"，用现成库容易被质疑没有自己的实现；
 * 2. 项目依赖版本全部锁定（离线环境），加新依赖有风险；
 * 3. 自己实现一个子集，反而更容易讲清楚原理，也方便写单元测试。
 *
 * 【Java 思维对照】
 * `object` = 只含静态方法的工具类；`sealed interface` + `when` 相当于
 * Java 的 sealed class + 模式匹配（Java 17 才有，Kotlin 一直有）。
 */
object MarkdownParser {

    /** 标题：1~6 个 #，后面至少一个空格。 */
    private val HEADING = Regex("^(#{1,6})\\s+(.*)$")

    /** 分割线：三个以上连续的 - * _，且整行只有它们（允许前后空白）。 */
    private val DIVIDER = Regex("^\\s*(?:-{3,}|\\*{3,}|_{3,})\\s*$")

    /** 引用：以 > 开头，后面可有空格。 */
    private val QUOTE = Regex("^>\\s?(.*)$")

    /** 无序列表：- * + 之一，后面至少一个空格。 */
    private val BULLET = Regex("^\\s*[-*+]\\s+(.*)$")

    /** 有序列表：数字 + . 或 ) + 空格。 */
    private val ORDERED = Regex("^\\s*(\\d{1,9})[.)]\\s+(.*)$")

    /** 围栏代码块标记。 */
    private const val FENCE = "```"

    /**
     * 解析入口。
     *
     * 逐行处理：先判断是否在代码块内，再依次匹配各种块级语法，
     * 都不匹配就当普通段落。
     */
    fun parse(source: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()

        // 代码块缓冲区：非 null 表示"当前正在 ``` 内部"
        var codeBuffer: StringBuilder? = null

        // 连续的非空普通行会合并成一个段落，这样段内换行不会被打散
        val paragraphBuffer = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraphBuffer.isNotEmpty()) {
                blocks += MdBlock.Paragraph(parseInline(paragraphBuffer.joinToString(" ")))
                paragraphBuffer.clear()
            }
        }

        source.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd()

            // ---------- 1. 代码块优先处理：内部内容不再解析 ----------
            if (line.trimStart().startsWith(FENCE)) {
                val buf = codeBuffer
                if (buf == null) {
                    // 进入代码块
                    flushParagraph()
                    codeBuffer = StringBuilder()
                } else {
                    // 结束代码块
                    blocks += MdBlock.CodeBlock(buf.toString().trimEnd('\n'))
                    codeBuffer = null
                }
                return@forEach
            }
            if (codeBuffer != null) {
                codeBuffer!!.append(line).append('\n')
                return@forEach
            }

            // ---------- 2. 空行：结束当前段落 ----------
            if (line.isBlank()) {
                flushParagraph()
                return@forEach
            }

            // ---------- 3. 分割线 ----------
            if (DIVIDER.matches(line)) {
                flushParagraph()
                blocks += MdBlock.Divider
                return@forEach
            }

            // ---------- 4. 标题 ----------
            HEADING.find(line)?.let { m ->
                flushParagraph()
                val level = m.groupValues[1].length
                blocks += MdBlock.Heading(level, parseInline(m.groupValues[2].trim()))
                return@forEach
            }

            // ---------- 5. 引用 ----------
            QUOTE.find(line)?.let { m ->
                flushParagraph()
                blocks += MdBlock.Quote(parseInline(m.groupValues[1]))
                return@forEach
            }

            // ---------- 6. 无序列表 ----------
            BULLET.find(line)?.let { m ->
                flushParagraph()
                blocks += MdBlock.Bullet(parseInline(m.groupValues[1]))
                return@forEach
            }

            // ---------- 7. 有序列表 ----------
            ORDERED.find(line)?.let { m ->
                flushParagraph()
                val number = m.groupValues[1].toIntOrNull() ?: 1
                blocks += MdBlock.Ordered(number, parseInline(m.groupValues[2]))
                return@forEach
            }

            // ---------- 8. 普通段落 ----------
            paragraphBuffer += line.trim()
        }

        // 收尾：未闭合的代码块也保留内容（用户可能正在输入）
        codeBuffer?.let { blocks += MdBlock.CodeBlock(it.toString().trimEnd('\n')) }
        flushParagraph()

        return blocks
    }

    /**
     * 解析行内标记。
     *
     * 扫描字符流，遇到标记符就尝试找配对的结束符：
     * - 找到 → 把中间的内容递归解析（以支持 `**粗体里有*斜体***`），并叠加样式
     * - 找不到 → 当作普通字符处理（这样用户输入半个 `**` 时不会吞掉后面的文字）
     */
    fun parseInline(text: String): List<MdInline> {
        val result = mutableListOf<MdInline>()
        val plain = StringBuilder()

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                result += MdInline(plain.toString())
                plain.clear()
            }
        }

        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                // ---------- 反引号：行内代码，内部不再解析 ----------
                c == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        flushPlain()
                        result += MdInline(text.substring(i + 1, end), setOf(InlineStyle.CODE))
                        i = end + 1
                    } else {
                        plain.append(c); i++
                    }
                }

                // ---------- ***粗斜体*** ----------
                text.startsWith("***", i) -> {
                    val end = text.indexOf("***", i + 3)
                    if (end > i) {
                        flushPlain()
                        result += parseInline(text.substring(i + 3, end)).map {
                            it.copy(styles = it.styles + InlineStyle.BOLD + InlineStyle.ITALIC)
                        }
                        i = end + 3
                    } else {
                        plain.append(c); i++
                    }
                }

                // ---------- **粗体** 或 __粗体__ ----------
                text.startsWith("**", i) || text.startsWith("__", i) -> {
                    val marker = text.substring(i, i + 2)
                    val end = text.indexOf(marker, i + 2)
                    if (end > i) {
                        flushPlain()
                        result += parseInline(text.substring(i + 2, end)).map {
                            it.copy(styles = it.styles + InlineStyle.BOLD)
                        }
                        i = end + 2
                    } else {
                        plain.append(c); i++
                    }
                }

                // ---------- ~~删除线~~ ----------
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end > i) {
                        flushPlain()
                        result += parseInline(text.substring(i + 2, end)).map {
                            it.copy(styles = it.styles + InlineStyle.STRIKETHROUGH)
                        }
                        i = end + 2
                    } else {
                        plain.append(c); i++
                    }
                }

                // ---------- *斜体* 或 _斜体_ ----------
                c == '*' || c == '_' -> {
                    val end = text.indexOf(c, i + 1)
                    if (end > i) {
                        flushPlain()
                        result += parseInline(text.substring(i + 1, end)).map {
                            it.copy(styles = it.styles + InlineStyle.ITALIC)
                        }
                        i = end + 1
                    } else {
                        plain.append(c); i++
                    }
                }

                // ---------- [文字](链接) ----------
                c == '[' -> {
                    val closeBracket = text.indexOf(']', i + 1)
                    val hasParen = closeBracket > i &&
                        closeBracket + 1 < text.length &&
                        text[closeBracket + 1] == '('
                    if (hasParen) {
                        val closeParen = text.indexOf(')', closeBracket + 2)
                        if (closeParen > closeBracket) {
                            flushPlain()
                            val label = text.substring(i + 1, closeBracket)
                            val url = text.substring(closeBracket + 2, closeParen)
                            result += parseInline(label).map { it.copy(linkUrl = url) }
                            i = closeParen + 1
                        } else {
                            plain.append(c); i++
                        }
                    } else {
                        plain.append(c); i++
                    }
                }

                else -> {
                    plain.append(c); i++
                }
            }
        }

        flushPlain()
        return result
    }

    /**
     * 去掉 Markdown 标记，得到纯文本。
     *
     * 用途：列表卡片的正文预览 —— 列表里没必要显示 `**` 这种符号。
     */
    fun toPlainText(source: String): String =
        parse(source).joinToString("\n") { block ->
            when (block) {
                is MdBlock.Heading -> inlineToPlain(block.inlines)
                is MdBlock.Paragraph -> inlineToPlain(block.inlines)
                is MdBlock.Bullet -> "• " + inlineToPlain(block.inlines)
                is MdBlock.Ordered -> "${block.number}. " + inlineToPlain(block.inlines)
                is MdBlock.Quote -> "▎ " + inlineToPlain(block.inlines)
                is MdBlock.CodeBlock -> block.code
                MdBlock.Divider -> "────────"
            }
        }

    private fun inlineToPlain(inlines: List<MdInline>): String =
        inlines.joinToString("") { it.text }
}
