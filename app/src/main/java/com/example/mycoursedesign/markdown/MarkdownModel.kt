package com.example.mycoursedesign.markdown

/**
 * Markdown 解析结果的数据模型。
 *
 * 【为什么要先把 Markdown 解析成结构化模型，而不是直接拼 SpannableString】
 * `SpannableString` 是 Android 类型，在 JVM 单元测试里用不了（会抛
 * `RuntimeException: Method ... not mocked`）。把"解析"和"渲染"拆开之后：
 *
 * - [MarkdownParser] 只依赖 Kotlin 标准库 → **可以直接写单元测试**，秒级运行
 * - [MarkdownRenderer] 才依赖 Android → 负责把模型变成带样式的文本
 *
 * 这和项目里 `CryptoEngine`（纯算法，可测）与 `CryptoManager`（依赖 Android，不可测）
 * 的拆法完全一致 —— 同一套思路复用在两个地方。
 *
 * 【Java 思维对照】
 * `sealed interface` + 多个 `data class` 相当于 Java 的 sealed 类层级 + 子类，
 * 配合 `when` 可以做到**穷尽匹配**（少写一个分支编译器就报错），
 * 比 `switch (type)` 加默认分支安全得多。
 */

/** 行内样式。一个片段可以同时拥有多种样式，例如粗斜体。 */
enum class InlineStyle {
    BOLD,
    ITALIC,
    STRIKETHROUGH,
    CODE
}

/**
 * 一段行内内容。
 *
 * @param text 显示文字（已经把 Markdown 标记符去掉）
 * @param styles 应用到这段文字上的样式
 * @param linkUrl 如果是链接，这里是目标地址；否则为 null
 */
data class MdInline(
    val text: String,
    val styles: Set<InlineStyle> = emptySet(),
    val linkUrl: String? = null
)

/**
 * 块级元素：标题、段落、列表项、引用、代码块、分割线。
 *
 * 每个块内部再持有若干 [MdInline]。
 */
sealed interface MdBlock {

    /** 标题，[level] 为 1~6（对应 # 的数量）。 */
    data class Heading(val level: Int, val inlines: List<MdInline>) : MdBlock

    /** 普通段落。 */
    data class Paragraph(val inlines: List<MdInline>) : MdBlock

    /** 无序列表项。 */
    data class Bullet(val inlines: List<MdInline>) : MdBlock

    /** 有序列表项，[number] 是原始序号。 */
    data class Ordered(val number: Int, val inlines: List<MdInline>) : MdBlock

    /** 引用块。 */
    data class Quote(val inlines: List<MdInline>) : MdBlock

    /** 围栏代码块（``` 包起来的内容），内部不再解析行内标记。 */
    data class CodeBlock(val code: String) : MdBlock

    /** 分割线（--- / *** / ___）。 */
    data object Divider : MdBlock
}
