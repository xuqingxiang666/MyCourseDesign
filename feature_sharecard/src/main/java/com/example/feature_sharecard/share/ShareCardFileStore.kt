package com.example.feature_sharecard.share

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * 分享文件的落地与 Uri 生成。
 *
 * 【为什么先写文件再分享，而不是直接传 Bitmap】
 * 跨进程传递大数据有限制（Intent 的 Binder 事务缓冲区约 1MB），
 * 一张 900×1200 的 ARGB 位图约 4.3MB，直接塞进 Intent 会抛
 * `TransactionTooLargeException`。
 * 所以标准做法是：写到文件 → 生成 content:// Uri → 只传 Uri。
 *
 * 【为什么用 cacheDir 而不是 filesDir】
 * 分享文件是一次性的：
 * - 放在 cacheDir，系统空间紧张时可以自动清理，不需要用户手动删
 * - 我们自己也会在每次写入前清理旧文件（见 [cleanupOldFiles]）
 *
 * 【Java 思维对照】
 * 相当于把 `File` + `FileProvider.getUriForFile(...)` 的组合封装成一个工具类。
 */
object ShareCardFileStore {

    private const val DIR_NAME = "share_cards"

    /** 最多保留多少个历史文件，超过就删最旧的。 */
    private const val MAX_FILES = 20

    /**
     * FileProvider 的 authority。
     *
     * 必须和 AndroidManifest 里声明的 `${applicationId}.sharecard.fileprovider` 一致。
     * 用包名拼出来而不是写死，这样模块被不同 App 复用时都正确。
     */
    fun authority(context: Context): String =
        "${context.packageName}.sharecard.fileprovider"

    /** 分享目录（不存在会创建）。 */
    private fun shareDir(context: Context): File =
        File(context.cacheDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /**
     * 把卡片位图写成 PNG，返回可分享的 Uri。
     *
     * @param note 用于生成文件名（含笔记 id）
     */
    fun saveCardImage(
        context: Context,
        note: ShareNote,
        bitmap: Bitmap,
        config: ShareCardConfig = ShareCardConfig()
    ): Uri {
        cleanupOldFiles(context)
        val file = File(shareDir(context), "card_${note.id}_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            // PNG 无损；质量参数对 PNG 无效，这里传 100 只是占位
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    /**
     * 把 CSV 文本写成文件，返回可分享的 Uri。
     *
     * 用 UTF-8 写，且文本本身已带 BOM（见 [CsvExporter]），
     * 这样 Excel 打开中文不乱码。
     */
    fun saveCsv(
        context: Context,
        csv: String,
        fileName: String = CsvExporter.suggestFileName()
    ): Uri {
        cleanupOldFiles(context)
        val file = File(shareDir(context), fileName)
        file.writeText(csv, Charsets.UTF_8)
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    /**
     * 清理旧文件，只保留最近的 [MAX_FILES] 个。
     *
     * 【为什么要做这件事】
     * 缓存目录虽然能被系统清理，但如果用户频繁分享，文件会越积越多，
     * 几百兆的缓存会让人以为 App 有内存泄漏。主动控制数量更省心。
     */
    fun cleanupOldFiles(context: Context) {
        val files = shareDir(context).listFiles()?.sortedByDescending { it.lastModified() }
            ?: return
        files.drop(MAX_FILES).forEach { runCatching { it.delete() } }
    }

    /** 当前分享目录里的文件数量（调试/测试用）。 */
    fun fileCount(context: Context): Int = shareDir(context).listFiles()?.size ?: 0
}
