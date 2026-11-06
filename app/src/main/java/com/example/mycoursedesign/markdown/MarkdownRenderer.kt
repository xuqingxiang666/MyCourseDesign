package com.example.mycoursedesign.markdown

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import android.util.TypedValue
import androidx.core.content.ContextCompat

/**
 * 把 Markdown 文本渲染成可直接放进 `TextView` 的富文本。
 *
 * 【职责划分】
 * - [MarkdownParser]：文本 → 结构化模型（纯 Kotlin，可单测）
 * - 本类：模型 → `SpannableString`（依赖 Android，不单测）
 *
 * 【为什么用 Spannable 而不是 WebView 或第三方库】
 * 1. 不引入新依赖（项目依赖版本锁定、离线环境）；
 * 2. 比 WebView 轻量得多，不会带来安全与性能问题；
 * 3. 能直接复用主题颜色，自动适配暗色模式。
 *
 * 【Java 思维对照】
 * `SpannableStringBuilder` 相当于 Java 里可变的 `SpannableStringBuilder`；
 * `setSpan(obj, start, end, flags)` 相当于给字符区间"贴标签"，
 * 渲染时 TextView 会遍历这些标签并调用对应 Span 的绘制方法。
 */
object MarkdownRenderer {

    /** 标题字号放大倍数（1~6 级）。 */
    private val HEADING_SCALE = floatArrayOf(1.60f, 1.42f, 1.26f, 1.14f, 1.06f, 1.0f)

    /**
     * 渲染入口。
     *
     * @param context 用来解析主题颜色（链接色、代码底色等），保证暗色模式下也协调
     */
    fun render(context: Context, source: String): CharSequence {
        val blocks = MarkdownParser.parse(source)
        val builder = SpannableStringBuilder()

        val linkColor = resolveColor(context, android.R.attr.textColorLink, 0xFF6750A4.toInt())
        val mutedColor = resolveColor(context, android.R.attr.textColorSecondary, 0xFF6B6B6B.toInt())
        val codeBackground = resolveColor(context, android.R.attr.colorBackground, 0x22808080)

        blocks.forEachIndexed { index, block ->
            if (index > 0) builder.append('\n')

            when (block) {
                is MdBlock.Heading -> {
                    val start = builder.length
                    appendInlines(builder, block.inlines, linkColor, codeBackground, forceBold = true)
                    val end = builder.length
                    if (end > start) {
                        val scale = HEADING_SCALE[(block.level - 1).coerceIn(0, 5)]
                        builder.setSpan(
                            RelativeSizeSpan(scale), start, end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }

                is MdBlock.Paragraph -> appendInlines(builder, block.inlines, linkColor, codeBackground)

                is MdBlock.Bullet -> {
                    appendMuted(builder, "• ", mutedColor)
                    appendInlines(builder, block.inlines, linkColor, codeBackground)
                }

                is MdBlock.Ordered -> {
                    appendMuted(builder, "${block.number}. ", mutedColor)
                    appendInlines(builder, block.inlines, linkColor, codeBackground)
                }

                is MdBlock.Quote -> {
                    // 左侧用竖线表示引用，颜色调淡
                    appendMuted(builder, "▎ ", mutedColor)
                    val start = builder.length
                    appendInlines(builder, block.inlines, linkColor, codeBackground)
                    val end = builder.length
                    if (end > start) {
                        builder.setSpan(
                            ForegroundColorSpan(mutedColor), start, end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }

                is MdBlock.CodeBlock -> {
                    val start = builder.length
                    builder.append(block.code)
                    val end = builder.length
                    if (end > start) {
                        builder.setSpan(
                            TypefaceSpan("monospace"), start, end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        builder.setSpan(
                            BackgroundColorSpan(codeBackground), start, end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }

                MdBlock.Divider -> appendMuted(builder, "────────", mutedColor)
            }
        }

        return builder
    }

    /** 追加行内内容，并为每个片段加样式。 */
    private fun appendInlines(
        builder: SpannableStringBuilder,
        inlines: List<MdInline>,
        linkColor: Int,
        codeBackground: Int,
        forceBold: Boolean = false
    ) {
        inlines.forEach { inline ->
            val start = builder.length
            builder.append(inline.text)
            val end = builder.length
            if (end <= start) return@forEach

            if (forceBold || inline.styles.contains(InlineStyle.BOLD)) {
                builder.setSpan(
                    StyleSpan(Typeface.BOLD), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (inline.styles.contains(InlineStyle.ITALIC)) {
                builder.setSpan(
                    StyleSpan(Typeface.ITALIC), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (inline.styles.contains(InlineStyle.STRIKETHROUGH)) {
                builder.setSpan(
                    StrikethroughSpan(), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (inline.styles.contains(InlineStyle.CODE)) {
                builder.setSpan(
                    TypefaceSpan("monospace"), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    BackgroundColorSpan(codeBackground), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            inline.linkUrl?.let { url ->
                builder.setSpan(
                    ForegroundColorSpan(linkColor), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    UnderlineSpan(), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // URLSpan 让文字可点击；需要 TextView 设置 movementMethod 才生效
                builder.setSpan(
                    URLSpan(url), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    /** 追加一段辅助文字（列表符号、引用线、分割线），用淡色。 */
    private fun appendMuted(builder: SpannableStringBuilder, text: String, color: Int) {
        val start = builder.length
        builder.append(text)
        builder.setSpan(
            ForegroundColorSpan(color), start, builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * 从主题里取颜色。
     *
     * 用主题属性而不是写死色值，暗色模式下才能自动变成合适的颜色。
     * 取不到时用 [fallback]。
     */
    private fun resolveColor(context: Context, attrRes: Int, fallback: Int): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attrRes, value, true)) {
            if (value.resourceId != 0) ContextCompat.getColor(context, value.resourceId) else value.data
        } else {
            fallback
        }
    }
}
