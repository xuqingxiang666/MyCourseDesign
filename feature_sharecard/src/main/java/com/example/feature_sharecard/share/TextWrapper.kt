package com.example.feature_sharecard.share

/**
 * 文本换行工具（纯逻辑，可单元测试）。
 *
 * 【为什么单独抽出来】
 * 画卡片时必须知道"这段文字在给定宽度内要折成几行"。
 * 如果直接写 `StaticLayout`，逻辑就跑进 Android 图形栈里，没法写普通单元测试。
 * 抽成"给定宽度和测量函数，返回若干行"的纯函数后：
 * - 生产代码传 `paint::measureText` 作为测量函数
 * - 单元测试传 `{ it.length * 10f }` 这样的假函数，**不需要 Android**
 *
 * 这是一个典型的"把平台依赖注入进来"的技巧：函数本身不依赖 Android，
 * 由调用方把"怎么量字宽"作为参数传进去。
 *
 * 【支持的能力】
 * - 按宽度自动折行（中文按字折、英文单词尽量不拆）
 * - 保留原文中的显式换行
 * - 限制最大行数，超出时最后一行加省略号
 */
object TextWrapper {

    /** 省略号。 */
    const val ELLIPSIS = "…"

    /**
     * 把文本折成若干行。
     *
     * @param text 原始文本（可含 `\n`）
     * @param maxWidth 每行最大宽度（单位与 [measure] 返回值一致）
     * @param measure 测量一段文本宽度；生产环境传 `Paint::measureText`
     * @param maxLines 最多保留几行；超出部分丢弃并加省略号
     */
    fun wrap(
        text: String,
        maxWidth: Float,
        measure: (String) -> Float,
        maxLines: Int = Int.MAX_VALUE
    ): List<String> {
        if (maxLines <= 0) return emptyList()
        if (text.isEmpty()) return listOf("")

        val lines = mutableListOf<String>()

        // 先按显式换行切段，段内再按宽度折行
        text.split('\n').forEach { paragraph ->
            if (paragraph.isEmpty()) {
                lines += ""
                return@forEach
            }

            val current = StringBuilder()
            var i = 0
            while (i < paragraph.length) {
                current.append(paragraph[i])
                // 超宽且本行不止一个字符时，退掉最后一个字符换行
                if (current.length > 1 && measure(current.toString()) > maxWidth) {
                    current.deleteCharAt(current.length - 1)
                    lines += current.toString()
                    current.clear()
                    // 注意：不推进 i，被退掉的那个字符要成为下一行的开头
                    continue
                }
                i++
            }
            if (current.isNotEmpty()) lines += current.toString()
        }

        if (lines.isEmpty()) lines += ""

        // 【为什么先完整收集再截断】
        // 如果折行过程中一到达 maxLines 就退出，就"不知道该不该加省略号"了
        // （无法区分"刚好这么多行"和"被截断了"）。先收齐再看是否超限，判断才准确。
        return if (lines.size > maxLines) {
            val kept = lines.take(maxLines).toMutableList()
            val last = kept.last()
            kept[kept.size - 1] = truncateWithEllipsis(last, maxWidth, measure)
            kept
        } else {
            // 最后一行也可能因为加了省略号而超宽（仅在截断场景），这里不处理
            lines.toList()
        }
    }

    /**
     * 在末尾加省略号；如果加完超宽，就不断去掉末尾字符直到放下。
     */
    private fun truncateWithEllipsis(
        line: String,
        maxWidth: Float,
        measure: (String) -> Float
    ): String {
        var candidate = line
        while (candidate.isNotEmpty() && measure(candidate + ELLIPSIS) > maxWidth) {
            candidate = candidate.dropLast(1)
        }
        return candidate + ELLIPSIS
    }
}
