package com.example.mycoursedesign.markdown

/**
 * Markdown 格式工具栏的纯逻辑实现。
 *
 * 【为什么单独抽出来】
 * "给选中文字加粗"本质是**字符串变换**：
 * 输入（原文、选区起点、选区终点）→ 输出（新文本、新选区）。
 * 这类逻辑没有任何 Android 依赖，可以写单元测试覆盖各种边界：
 * 未选中、已加粗要取消、多行选择、行首有空格、选区在文首/文末……
 *
 * 有了它，Activity 里的按钮回调只需要一行：
 * ```kotlin
 * applyFormat { text, s, e -> MarkdownFormatter.toggleBold(text, s, e) }
 * ```
 *
 * 【设计原则：只改文本，不产生富文本对象】
 * 操作结果仍然是 **Markdown 纯文本**，所以数据库格式不用改、渲染器不用改、
 * 之前写的 27 个解析器测试也依然有效。
 *
 * 【Java 思维对照】
 * `object` = 静态工具类；[Result] 是不可变的"操作结果"值对象，
 * 相当于 Java 里返回 `Pair<String, IntRange>`，但用数据类更清晰。
 */
object MarkdownFormatter {

    /**
     * 一行在原文中的位置。
     *
     * 用自定义数据类而不是 Kotlin 的 `IntRange`，因为 `IntRange` 的语义是"闭区间"，
     * 而这里需要"左闭右开"（[start, end)），直接用 `until` 容易和 `first/last`
     * 混淆 —— 实测就踩过这个坑。
     */
    internal data class LineRange(val start: Int, val end: Int)

    /** 格式化操作的结果：新文本 + 操作后应当保持的选区。 */
    data class Result(
        val text: String,
        val selectionStart: Int,
        val selectionEnd: Int
    )

    // ==================== 行内样式 ====================

    /** 粗体 `**文字**` */
    fun toggleBold(text: String, start: Int, end: Int): Result =
        toggleWrap(text, start, end, "**")

    /** 斜体 `*文字*` */
    fun toggleItalic(text: String, start: Int, end: Int): Result =
        toggleWrap(text, start, end, "*")

    /** 删除线 `~~文字~~` */
    fun toggleStrikethrough(text: String, start: Int, end: Int): Result =
        toggleWrap(text, start, end, "~~")

    /** 行内代码 `` `文字` `` */
    fun toggleInlineCode(text: String, start: Int, end: Int): Result =
        toggleWrap(text, start, end, "`")

    // ==================== 块级：列表 ====================

    /** 无序列表：给选中的每一行加 `- ` 前缀；全部已有则去掉。 */
    fun toggleBullet(text: String, start: Int, end: Int): Result =
        toggleLinePrefix(text, start, end, bullet = true)

    /** 有序列表：给选中的每一行加 `1. `、`2. `…；全部已有则去掉。 */
    fun toggleOrdered(text: String, start: Int, end: Int): Result =
        toggleLinePrefix(text, start, end, bullet = false)

    // ==================== 标题 ====================

    /**
     * 设置标题级别：`#` × level。再点同一级别则取消（变回普通段落）。
     *
     * @param level 1~6
     */
    fun toggleHeading(text: String, start: Int, end: Int, level: Int): Result {
        val safeLevel = level.coerceIn(1, 6)
        val lines = lineRanges(text, start, end)
        if (lines.isEmpty()) return Result(text, start, end)

        val prefix = "#".repeat(safeLevel) + " "
        val firstLine = text.substring(lines.first().start, lines.first().end)

        // 已经是这个级别 -> 去掉前缀
        val removeMode = firstLine.startsWith(prefix)

        val builder = StringBuilder()
        var newStart = -1
        var delta = 0
        var cursor = 0

        lines.forEach { range ->
            val lineText = text.substring(range.start, range.end)
            builder.append(text, cursor, range.start)

            val newLine = when {
                removeMode && lineText.startsWith(prefix) -> lineText.removePrefix(prefix)
                removeMode -> lineText
                // 先去掉已有的 # 前缀，再套新的
                else -> prefix + lineText.replaceFirst(Regex("^#{1,6}\\s+"), "")
            }

            if (newStart < 0) newStart = builder.length
            builder.append(newLine)
            delta += newLine.length - lineText.length
            cursor = range.end
        }
        builder.append(text, cursor, text.length)

        val resultStart = (newStart).coerceAtLeast(0)
        return Result(builder.toString(), resultStart, resultStart + lines.sumOf { it.end - it.start } + delta)
    }

    // ==================== 内部实现 ====================

    /**
     * 通用的"包裹/取消包裹"逻辑。
     *
     * 判定顺序：
     * 1. **选区已经紧邻两侧的标记**（如 `**|文字|**`）→ 取消包裹
     * 2. 选区为空 → 插入一对标记，光标放中间（方便接着打字）
     * 3. 选区跨多行 → 逐行包裹
     * 4. 其他 → 用标记包裹选区
     */
    private fun toggleWrap(text: String, start: Int, end: Int, marker: String): Result {
        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(safeStart, text.length)

        val before = text.substring(0, safeStart)
        val selected = text.substring(safeStart, safeEnd)
        val after = text.substring(safeEnd)

        // 1. 已包裹 -> 取消
        if (before.endsWith(marker) && after.startsWith(marker)) {
            val newText = before.dropLast(marker.length) + selected + after.substring(marker.length)
            val newStart = safeStart - marker.length
            return Result(newText, newStart, newStart + selected.length)
        }

        // 2. 空选区 -> 插入成对标记
        if (selected.isEmpty()) {
            val newText = before + marker + marker + after
            val cursor = safeStart + marker.length
            return Result(newText, cursor, cursor)
        }

        // 3. 多行 -> 逐行包裹（Markdown 的 `**` 不能跨行生效）
        if (selected.contains('\n')) {
            val lines = lineRanges(text, safeStart, safeEnd)
            val builder = StringBuilder()
            var cursor = 0
            var delta = 0
            var newStart = -1
            lines.forEach { range ->
                val lineText = text.substring(range.start, range.end)
                builder.append(text, cursor, range.start)
                if (lineText.isNotBlank()) {
                    if (lineText.startsWith(marker) && lineText.endsWith(marker) && lineText.length > marker.length * 2) {
                        // 该行已包裹 -> 去掉
                        val inner = lineText.substring(marker.length, lineText.length - marker.length)
                        if (newStart < 0) newStart = builder.length
                        builder.append(inner)
                        delta -= marker.length * 2
                    } else {
                        if (newStart < 0) newStart = builder.length
                        builder.append(marker).append(lineText).append(marker)
                        delta += marker.length * 2
                    }
                } else {
                    if (newStart < 0) newStart = builder.length
                    builder.append(lineText)
                }
                cursor = range.end
            }
            builder.append(text, cursor, text.length)
            val rs = newStart.coerceAtLeast(0)
            return Result(builder.toString(), rs, rs + (safeEnd - safeStart) + delta)
        }

        // 4. 单行普通情况
        val newText = before + marker + selected + marker + after
        return Result(newText, safeStart + marker.length, safeStart + marker.length + selected.length)
    }

    /**
     * 列表前缀的添加/移除。
     *
     * 规则：只要选中范围内**所有非空行**都已经有列表前缀，就整体移除；否则整体添加。
     * 这样行为可预测，不会出现"部分加了部分没加"的混乱状态。
     */
    private fun toggleLinePrefix(text: String, start: Int, end: Int, bullet: Boolean): Result {
        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(safeStart, text.length)
        val lines = lineRanges(text, safeStart, safeEnd)
        if (lines.isEmpty()) return Result(text, safeStart, safeEnd)

        val nonEmpty = lines.filter { text.substring(it.start, it.end).isNotBlank() }
        if (nonEmpty.isEmpty()) return Result(text, safeStart, safeEnd)

        val alreadyPrefixed = nonEmpty.all { range ->
            val line = text.substring(range.start, range.end)
            if (bullet) line.startsWith("- ") else Regex("^\\d+[.)]\\s+").containsMatchIn(line)
        }

        val builder = StringBuilder()
        var cursor = 0
        var delta = 0
        var newStart = -1
        var index = 1

        lines.forEach { range ->
            val lineText = text.substring(range.start, range.end)
            builder.append(text, cursor, range.start)
            if (newStart < 0) newStart = builder.length

            val newLine = when {
                lineText.isBlank() -> lineText
                alreadyPrefixed -> if (bullet) {
                    lineText.removePrefix("- ")
                } else {
                    lineText.replaceFirst(Regex("^\\d+[.)]\\s+"), "")
                }
                else -> {
                    // 先去掉重复前缀，避免 "- - 文字"
                    val cleaned = lineText.removePrefix("- ")
                        .replaceFirst(Regex("^\\d+[.)]\\s+"), "")
                    if (bullet) "- $cleaned" else "${index++}. $cleaned"
                }
            }
            builder.append(newLine)
            delta += newLine.length - lineText.length
            cursor = range.end
        }
        builder.append(text, cursor, text.length)

        val rs = newStart.coerceAtLeast(0)
        return Result(builder.toString(), rs, rs + (safeEnd - safeStart) + delta)
    }

    /**
     * 找出与 [start, end) 相交的所有行的范围（含行首行尾，不含换行符）。
     *
     * 空选区时返回光标所在的那一行。
     */
    internal fun lineRanges(text: String, start: Int, end: Int): List<LineRange> {
        if (text.isEmpty()) return listOf(LineRange(0, 0))

        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(safeStart, text.length)

        val firstLineStart = text.lastIndexOf('\n', (safeStart - 1).coerceAtLeast(0))
            .let { if (it < 0 || safeStart == 0) 0 else it + 1 }

        val result = mutableListOf<LineRange>()
        var cursor = firstLineStart
        var limit = safeEnd

        while (cursor <= text.length) {
            val nextBreak = text.indexOf('\n', cursor)
            val lineEnd = if (nextBreak < 0) text.length else nextBreak
            result += LineRange(cursor, maxOf(lineEnd, cursor))

            if (nextBreak < 0 || lineEnd >= limit) break
            cursor = nextBreak + 1
            // 选区正好结束在行首时，不要多算下一行
            if (cursor > safeEnd) break
        }
        return result
    }
}
