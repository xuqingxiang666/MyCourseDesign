package com.example.feature_sharecard.share

import android.graphics.Bitmap

/**
 * [ShareCardPlugin] 的实现。
 *
 * 【为什么用 object 而不是 class】
 * 插件是无状态的能力集合（画卡片、导 CSV 都不需要保存成员变量），
 * 用单例最省事，调用方直接 `ShareCardPluginImpl.exportCsv(...)`。
 *
 * 如果将来插件需要 Context（比如写缓存文件、取 FileProvider Uri），
 * 就改成 `class ShareCardPluginImpl(private val context: Context)`，
 * 由 App 在启动时创建 —— 接口签名不变，调用方影响很小。
 */
object ShareCardPluginImpl : ShareCardPlugin {

    override fun exportCsv(notes: List<ShareNote>): String = CsvExporter.export(notes)

    override fun renderCard(note: ShareNote, config: ShareCardConfig): Bitmap =
        ShareCardRenderer.render(note, config)
}
