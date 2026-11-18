package com.example.feature_sharecard.share

import android.graphics.Bitmap

/**
 * 分享卡片插件对外的统一契约。
 *
 * 【为什么先定义接口，再写实现】
 * 主 App（`app` 模块）只依赖这个接口，不关心卡片是 Canvas 画的还是别的方式画的。
 * 将来若要换实现（比如支持主题皮肤、换成 Compose 绘制），只要新写一个实现类，
 * App 侧代码不用动 —— 这就是"模块间通过契约解耦"。
 *
 * 【能力会随提交逐步增加】
 * - 第一次提交：`exportCsv`
 * - 本次：`renderCard`（生成 Bitmap）
 * - 后续：`shareCard`（通过 FileProvider 分享成文件）
 *
 * 之所以不在第一个提交里就把所有方法声明出来，是因为**每个提交都必须能编译通过**：
 * 声明了却没人实现，编译就不过。
 */
interface ShareCardPlugin {

    /**
     * 把笔记列表导出为 CSV 文本（含 UTF-8 BOM）。
     *
     * 纯字符串处理，不涉及文件读写 —— 写文件由调用方负责，
     * 这样插件不需要知道存储权限、目录结构这些细节。
     */
    fun exportCsv(notes: List<ShareNote>): String

    /**
     * 把一条笔记渲染成分享卡片图片。
     *
     * @param config 样式配置；不传则使用默认紫色主题与 900×1200 尺寸
     */
    fun renderCard(note: ShareNote, config: ShareCardConfig = ShareCardConfig()): Bitmap
}
