package com.example.feature_sharecard.share

/**
 * 分享卡片的样式配置。
 *
 * 【为什么把样式做成参数而不是写死在绘制代码里】
 * 1. 主 App 支持暗色主题，卡片背景/文字颜色需要跟随主题传入；
 * 2. 尺寸做成参数，方便按不同分享场景（朋友圈竖图 / 聊天横图）调整；
 * 3. 有了默认值，调用方不传也能生成一张可用的卡片。
 *
 * 颜色统一用 ARGB Int，和 Android 的 `Color` / `?attr` 解析结果直接对接。
 */
data class ShareCardConfig(

    /** 卡片宽度（像素）。默认 900×1200 是适合聊天分享的 3:4 竖图。 */
    val width: Int = 900,

    /** 卡片高度（像素）。 */
    val height: Int = 1200,

    /** 背景渐变色（起点）。默认使用 App 主题紫色。 */
    val backgroundStart: Int = 0xFF6750A4.toInt(),

    /** 背景渐变色（终点）。 */
    val backgroundEnd: Int = 0xFFB69DF8.toInt(),

    /** 内层白卡片颜色。 */
    val cardColor: Int = 0xFFFFFFFF.toInt(),

    /** 标题文字颜色。 */
    val titleColor: Int = 0xFF1C1B1F.toInt(),

    /** 正文文字颜色。 */
    val bodyColor: Int = 0xFF49454F.toInt(),

    /** 页脚文字颜色（时间、App 名）。 */
    val footerColor: Int = 0xFF79747E.toInt(),

    /** 强调色（左侧竖条、小标签）。 */
    val accentColor: Int = 0xFF6750A4.toInt(),

    /** 卡片上显示的 App 名称。 */
    val appName: String = "MyCourseDesign",

    /** 顶部小标签文字。 */
    val label: String = "我的笔记",

    /** 正文最多显示几行，超出用省略号。 */
    val maxBodyLines: Int = 12,

    /** 标题最多显示几行。 */
    val maxTitleLines: Int = 2
)
