package com.example.feature_sharecard.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 把一条笔记画成一张可分享的图片。
 *
 * 【为什么用 Canvas 手绘，而不是截图 View 或 WebView】
 * 1. **不依赖布局**：不需要把界面渲染出来再截屏，后台线程也能画；
 * 2. **结果确定**：同一份数据每次画出来都一样，便于测试与复用；
 * 3. **轻量**：WebView 太重，而且引入额外复杂度（课程设计没必要）。
 *
 * 【版式（全部按宽度比例计算，换尺寸不用改代码）】
 * ```
 * ┌──────────────────────────┐  ← 渐变背景
 *  │ ┌──────────────────────┐ │
 *  │ │ ▌我的笔记             │ │  ← 顶部小标签
 *  │ │ 标题（最多 2 行）      │ │
 *  │ │ ──────────────────── │ │  ← 分割线
 *  │ │ 正文（最多 12 行）     │ │
 *  │ │                      │ │
 *  │ │ 2026-11-18 19:00     │ │  ← 页脚
 *  │ │           MyCourseDesign │
 *  │ └──────────────────────┘ │
 *  └──────────────────────────┘
 * ```
 *
 * 【Java 思维对照】
 * 相当于 `Graphics2D` 那一套：`Bitmap` 是画布缓冲区，`Canvas` 是绘图上下文，
 * `Paint` 是画笔（颜色、字号、抗锯齿）。
 */
object ShareCardRenderer {

    /**
     * 渲染卡片。
     *
     * @param note 笔记数据（正文应为明文；加密笔记由调用方决定是否允许分享）
     * @param config 样式配置，默认使用 900×1200 的紫色主题
     */
    fun render(note: ShareNote, config: ShareCardConfig = ShareCardConfig()): Bitmap {
        val width = config.width
        val height = config.height
        val w = width.toFloat()
        val h = height.toFloat()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // ---------- 1. 渐变背景 ----------
        val background = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, w, h,
                config.backgroundStart, config.backgroundEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, background)

        // ---------- 2. 内层卡片 ----------
        val inset = w * 0.055f
        val radius = w * 0.05f
        val cardRect = RectF(inset, inset, w - inset, h - inset)
        val cardPaint = Paint().apply {
            isAntiAlias = true
            color = config.cardColor
        }
        canvas.drawRoundRect(cardRect, radius, radius, cardPaint)

        // ---------- 3. 文字与留白 ----------
        val padding = w * 0.075f
        val contentLeft = cardRect.left + padding
        val contentRight = cardRect.right - padding
        val contentWidth = contentRight - contentLeft

        // 顶部小标签
        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = config.accentColor
            textSize = w * 0.034f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        var cursorY = cardRect.top + padding + labelPaint.textSize
        canvas.drawText(config.label, contentLeft + w * 0.028f, cursorY, labelPaint)

        // 标签左边的小竖条
        val barPaint = Paint().apply {
            isAntiAlias = true
            color = config.accentColor
        }
        canvas.drawRoundRect(
            RectF(
                contentLeft,
                cursorY - labelPaint.textSize,
                contentLeft + w * 0.012f,
                cursorY + w * 0.004f
            ),
            w * 0.006f, w * 0.006f, barPaint
        )

        // ---------- 4. 标题 ----------
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = config.titleColor
            textSize = w * 0.072f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleLineHeight = titlePaint.textSize * 1.35f
        cursorY += titlePaint.textSize * 1.6f

        val titleLines = TextWrapper.wrap(
            note.title.ifBlank { "（无标题）" },
            contentWidth,
            { titlePaint.measureText(it) },
            config.maxTitleLines
        )
        titleLines.forEach { line ->
            canvas.drawText(line, contentLeft, cursorY, titlePaint)
            cursorY += titleLineHeight
        }

        // ---------- 5. 分割线 ----------
        cursorY += w * 0.01f
        val dividerPaint = Paint().apply {
            isAntiAlias = true
            color = config.accentColor
            alpha = 60
            strokeWidth = w * 0.003f
        }
        canvas.drawLine(contentLeft, cursorY, contentRight, cursorY, dividerPaint)
        cursorY += w * 0.05f

        // ---------- 6. 正文 ----------
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            color = config.bodyColor
            textSize = w * 0.045f
        }
        val bodyLineHeight = bodyPaint.textSize * 1.55f

        // 正文可用高度：扣掉页脚区域
        val footerReserve = w * 0.22f
        val maxBodyHeight = cardRect.bottom - padding - footerReserve - cursorY
        val maxLinesByHeight = (maxBodyHeight / bodyLineHeight).toInt().coerceAtLeast(1)
        val bodyLineLimit = minOf(config.maxBodyLines, maxLinesByHeight)

        val bodyLines = TextWrapper.wrap(
            note.content.ifBlank { "（暂无内容）" },
            contentWidth,
            { bodyPaint.measureText(it) },
            bodyLineLimit
        )
        bodyLines.forEach { line ->
            if (cursorY > cardRect.bottom - padding - footerReserve) return@forEach
            canvas.drawText(line, contentLeft, cursorY, bodyPaint)
            cursorY += bodyLineHeight
        }

        // ---------- 7. 页脚 ----------
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = config.footerColor
            textSize = w * 0.032f
        }
        val footerBaseline = cardRect.bottom - padding * 0.6f

        val dateText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(note.lastModified))
        canvas.drawText(dateText, contentLeft, footerBaseline, footerPaint)

        // App 名右对齐
        val appWidth = footerPaint.measureText(config.appName)
        canvas.drawText(config.appName, contentRight - appWidth, footerBaseline, footerPaint)

        return bitmap
    }
}
