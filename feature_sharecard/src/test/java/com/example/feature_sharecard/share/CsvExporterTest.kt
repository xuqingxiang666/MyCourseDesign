package com.example.feature_sharecard.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/**
 * [CsvExporter] 的单元测试。
 *
 * 这是纯 JVM 逻辑，不依赖 Android，跑一次几十毫秒。
 * 重点覆盖三个"中文 CSV 必踩的坑"：BOM、CRLF、字段转义。
 */
class CsvExporterTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun note(
        id: Long = 1,
        title: String = "标题",
        content: String = "内容",
        lastModified: Long = 0L
    ) = ShareNote(id, title, content, lastModified)

    // ==================== BOM 与表头 ====================

    @Test
    fun `starts with utf8 bom`() {
        val csv = CsvExporter.export(emptyList(), utc)
        assertTrue("文件必须以 BOM 开头，否则 Excel 中文乱码", csv.startsWith("\uFEFF"))
    }

    @Test
    fun `bom is only at the beginning`() {
        val csv = CsvExporter.export(listOf(note()), utc)
        assertEquals(
            "BOM 只应出现一次",
            1,
            csv.count { it == '\uFEFF' }
        )
    }

    @Test
    fun `header row is present`() {
        val csv = CsvExporter.export(emptyList(), utc)
        assertEquals("\uFEFFid,title,content,lastModified\r\n", csv)
    }

    // ==================== 行分隔符 ====================

    @Test
    fun `uses crlf line endings`() {
        val csv = CsvExporter.export(listOf(note()), utc)
        // 去掉所有 CRLF 后不应再有单独的 \n
        assertFalse("不应出现孤立的 \\n", csv.replace("\r\n", "").contains('\n'))
        assertTrue(csv.contains("\r\n"))
    }

    // ==================== 内容与转义 ====================

    @Test
    fun `plain fields are not quoted`() {
        val csv = CsvExporter.export(listOf(note(id = 7, title = "买菜", content = "土豆")), utc)
        assertTrue(csv.contains("7,买菜,土豆,"))
    }

    @Test
    fun `chinese characters are preserved`() {
        val csv = CsvExporter.export(listOf(note(title = "会议纪要", content = "讨论项目进度")), utc)
        assertTrue(csv.contains("会议纪要"))
        assertTrue(csv.contains("讨论项目进度"))
    }

    @Test
    fun `comma is escaped with quotes`() {
        val csv = CsvExporter.export(listOf(note(title = "买了苹果,香蕉")), utc)
        assertTrue("含逗号的字段必须加引号", csv.contains("\"买了苹果,香蕉\""))
    }

    @Test
    fun `quotes are doubled`() {
        assertEquals("\"他说\"\"你好\"\"\"", CsvExporter.escape("他说\"你好\""))
    }

    @Test
    fun `newline is quoted`() {
        val escaped = CsvExporter.escape("第一行\n第二行")
        assertTrue(escaped.startsWith("\""))
        assertTrue(escaped.endsWith("\""))
        assertTrue(escaped.contains("\n"))
    }

    @Test
    fun `carriage return is quoted`() {
        assertTrue(CsvExporter.escape("a\rb").startsWith("\""))
    }

    @Test
    fun `field without special chars is untouched`() {
        assertEquals("普通内容", CsvExporter.escape("普通内容"))
    }

    // ==================== 时间列 ====================

    @Test
    fun `timestamp is formatted in given time zone`() {
        // 2024-01-01 00:00:00 UTC
        val millis = 1_704_067_200_000L
        val csv = CsvExporter.export(listOf(note(id = 1, lastModified = millis)), utc)
        assertTrue("时间列应按 UTC 格式化: $csv", csv.contains("2024-01-01 00:00:00"))
    }

    // ==================== 多条与空列表 ====================

    @Test
    fun `empty list produces header only`() {
        val csv = CsvExporter.export(emptyList(), utc)
        // BOM + 表头 + 一个 CRLF
        assertEquals(1, csv.split("\r\n").size - 1)
    }

    @Test
    fun `multiple notes produce one line each`() {
        val csv = CsvExporter.export(
            listOf(note(id = 1, title = "A"), note(id = 2, title = "B"), note(id = 3, title = "C")),
            utc
        )
        val lines = csv.removePrefix("\uFEFF").split("\r\n").filter { it.isNotEmpty() }
        assertEquals("表头 + 3 行数据", 4, lines.size)
        assertTrue(lines[1].startsWith("1,A"))
        assertTrue(lines[3].startsWith("3,C"))
    }

    @Test
    fun `suggested file name ends with csv`() {
        val name = CsvExporter.suggestFileName(utc)
        assertTrue(name, name.endsWith(".csv"))
        assertTrue(name, name.startsWith("notes_"))
    }
}
