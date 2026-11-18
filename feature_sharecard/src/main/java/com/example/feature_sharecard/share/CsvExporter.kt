package com.example.feature_sharecard.share

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * CSV 导出器。
 *
 * 【为什么单元测试都围绕这个类】
 * 它是**纯逻辑**（只依赖 JDK 的字符串与日期格式化），不碰 Android，
 * 可以在 JVM 上直接跑测试，非常适合作为"单元测试覆盖率"的素材。
 *
 * 【三个关键细节（也是课设要求里点名要的）】
 *
 * 1. **UTF-8 BOM**
 *    Excel 打开 UTF-8 的 CSV 时，如果没有 BOM，中文会显示成乱码
 *    （Excel 会按本地编码 GBK 去猜）。开头写一个 `\uFEFF` 就能正确识别。
 *    这是"导出中文 CSV"最常见的坑。
 *
 * 2. **CRLF 换行**
 *    CSV 规范（RFC 4180）要求行分隔用 `\r\n`。Excel 与多数表格软件都按这个来，
 *    只写 `\n` 在某些版本上会把多行内容合并成一行。
 *
 * 3. **字段转义**
 *    当内容包含逗号、双引号、换行时，必须用双引号包裹整个字段，
 *    并把内部的双引号写成两个（`"` → `""`）。
 *    否则带逗号的标题会把列数撑乱，导致表格错位。
 */
object CsvExporter {

    /** UTF-8 BOM。Excel 靠它识别文件是 UTF-8，否则中文乱码。 */
    const val UTF8_BOM = "\uFEFF"

    /** RFC 4180 规定的行分隔符。 */
    private const val CRLF = "\r\n"

    /** 表头。 */
    private const val HEADER = "id,title,content,lastModified"

    /**
     * 导出为 CSV 文本。
     *
     * @param notes 要导出的笔记（调用方负责过滤掉加密笔记，见 [ShareNote.content]）
     * @param timeZone 时间列使用的时区，默认本机；测试传 UTC 以保证结果稳定
     */
    fun export(
        notes: List<ShareNote>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            this.timeZone = timeZone
        }

        val builder = StringBuilder()
        // ★ BOM 必须是文件最前面，且在表头之前
        builder.append(UTF8_BOM)
        builder.append(HEADER).append(CRLF)

        notes.forEach { note ->
            builder.append(note.id).append(',')
            builder.append(escape(note.title)).append(',')
            builder.append(escape(note.content)).append(',')
            builder.append(formatter.format(Date(note.lastModified)))
            builder.append(CRLF)
        }

        return builder.toString()
    }

    /**
     * 转义一个字段。
     *
     * 规则：
     * - 不含特殊字符 → 原样输出（保持文件干净）
     * - 含 `,` `"` `\n` `\r` → 用双引号包裹，内部 `"` 变 `""`
     */
    fun escape(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return value
        return '"' + value.replace("\"", "\"\"") + '"'
    }

    /**
     * 建议的文件名。
     *
     * 带时间戳，避免多次导出互相覆盖。
     */
    fun suggestFileName(timeZone: TimeZone = TimeZone.getDefault()): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).apply {
            this.timeZone = timeZone
        }
        return "notes_${formatter.format(Date())}.csv"
    }
}
