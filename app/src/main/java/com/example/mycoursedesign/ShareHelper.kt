package com.example.mycoursedesign

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.core.data.entity.Note
import com.example.mycoursedesign.markdown.MarkdownParser
import com.example.feature_sharecard.share.ShareCardPluginImpl
import com.example.feature_sharecard.share.ShareNote

/**
 * 把"分享"这件事从 Activity 里抽出来的小工具。
 *
 * 【职责】
 * 1. 把 core 的 [Note] 转换成插件需要的 [ShareNote]（跨模块只传 DTO）
 * 2. 处理**加密笔记不能分享**这条业务规则
 * 3. 组装 ACTION_SEND Intent 并启动系统分享面板
 *
 * 【为什么不把这个逻辑放进插件模块】
 * "加密笔记要不要分享、怎么提示用户"是**本 App 的业务规则**，
 * 不是插件的通用能力。插件只负责"给我数据，我给你图/文件"，
 * 保持无状态、可复用。
 */
object ShareHelper {

    /** 分享结果，供调用方决定提示什么文案。 */
    sealed interface Outcome {
        /** 成功。@param skipped 因加密而跳过、未参与分享的条数 */
        data class Success(val exported: Int, val skipped: Int = 0) : Outcome

        /** 加密笔记被拦截（单条分享时）。 */
        data object BlockedEncrypted : Outcome

        /** 没有可分享的内容。 */
        data object NothingToShare : Outcome

        /** 出错，附原因。 */
        data class Failed(val message: String) : Outcome
    }

    /**
     * 把数据库实体转换成插件 DTO。
     *
     * ★ 正文先转成纯文本再交给插件。
     * 卡片是给人看的图片，不该出现 `#` `**` `-` 这些 Markdown 标记。
     * 用 App 里已有的 [MarkdownParser.toPlainText]（有 27 个单元测试覆盖），
     * 插件本身保持"只负责画"，不掺业务规则 —— 这样插件换给别的 App 也能用。
     *
     * 标题为空时补「（无标题）」，避免卡片上出现一片空白。
     */
    private fun Note.toShareNote() = ShareNote(
        id = id,
        title = title.ifBlank { "（无标题）" },
        content = MarkdownParser.toPlainText(content),
        lastModified = lastModified
    )

    /**
     * 分享单条笔记的卡片。
     *
     * ★ 加密笔记直接拦截：
     * 卡片上要画**明文**，而加密笔记的 content 是密文，插件没有密钥也解不开。
     * 就算强行画，画出来也是乱码；更糟的是如果这里偷偷解密，
     * 密文就会被写入缓存文件，等于绕过了加密保护。所以一律拒绝，并提示先取消加密。
     */
    fun shareCard(context: Context, note: Note): Outcome {
        if (note.isEncrypted) return Outcome.BlockedEncrypted

        return runCatching {
            val uri = ShareCardPluginImpl.createCardUri(context, note.toShareNote())
            launch(
                context = context,
                intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TITLE, note.title)
                    // 给接收方临时读权限，否则对方打不开这个 content:// Uri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    // 部分系统/应用只认 ClipData 里的 Uri 授权
                    clipData = ClipData.newRawUri("share_card", uri)
                },
                chooserTitle = context.getString(R.string.share_chooser_card)
            )
            Outcome.Success(exported = 1)
        }.getOrElse { e ->
            Outcome.Failed(e.message ?: e::class.java.simpleName)
        }
    }

    /**
     * 导出全部笔记为 CSV 并分享。
     *
     * ★ 加密笔记不导出：
     * CSV 是导出给人看的，密文（Base64 乱码）对人毫无意义，
     * 而且等于把密文散播出去。所以过滤掉，并在提示里说明跳过了几条。
     */
    fun shareCsv(context: Context, notes: List<Note>): Outcome {
        val exportable = notes.filter { !it.isEncrypted }
        if (exportable.isEmpty()) return Outcome.NothingToShare

        val skipped = notes.size - exportable.size

        return runCatching {
            val uri = ShareCardPluginImpl.createCsvUri(context, exportable.map { it.toShareNote() })
            launch(
                context = context,
                intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = ClipData.newRawUri("notes_csv", uri)
                },
                chooserTitle = context.getString(R.string.share_chooser_csv)
            )
            Outcome.Success(exported = exportable.size, skipped = skipped)
        }.getOrElse { e ->
            Outcome.Failed(e.message ?: e::class.java.simpleName)
        }
    }

    private fun launch(context: Context, intent: Intent, chooserTitle: String) {
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    /** 把结果转成一条提示。 */
    fun toastFor(context: Context, outcome: Outcome) {
        val text = when (outcome) {
            is Outcome.Success ->
                if (outcome.skipped > 0) {
                    context.getString(R.string.share_csv_done_with_skip, outcome.exported, outcome.skipped)
                } else {
                    context.getString(R.string.share_done, outcome.exported)
                }

            Outcome.BlockedEncrypted -> context.getString(R.string.share_encrypted_blocked)
            Outcome.NothingToShare -> context.getString(R.string.share_nothing)
            is Outcome.Failed -> context.getString(R.string.share_failed, outcome.message)
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}
