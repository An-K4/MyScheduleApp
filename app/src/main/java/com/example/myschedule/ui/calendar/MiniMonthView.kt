package com.example.myschedule.ui.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import androidx.core.graphics.toColorInt

/**
 * Custom View vẽ mini calendar bằng Canvas.
 * Thay thế GridLayout + TextView động → giảm View count từ ~35 xuống 1.
 *
 * Tuần bắt đầu từ Thứ 2 (Mon).
 * Màu text dùng colorOnBackground từ Material Theme.
 * Chiều cao tự tính theo số hàng thực tế của tháng.
 */

class MiniMonthView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Data ────────────────────────────────────────────────────────────────
    private var month: YearMonth = YearMonth.now()
    private var today: LocalDate = LocalDate.now()
    private var eventDates: Set<LocalDate> = emptySet()

    // ── Geometry ────────────────────────────────────────────────────────────
    private var cellSize = 0f
    private var rowCount = 0

    // ── Paint objects (tái sử dụng, không new trong onDraw) ─────────────────
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }

    private val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFA500".toColorInt()
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#03DAC5".toColorInt()
        style = Paint.Style.FILL
    }

    // ── Colors (resolved once per bind) ─────────────────────────────────────
    private var colorNormal = Color.BLACK
    private var colorSunday = "#E53935".toColorInt()
    private var colorToday = Color.WHITE

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Gọi từ ViewHolder.bind().
     * Không tạo View mới — chỉ cập nhật data và invalidate.
     */
    fun bind(
        month: YearMonth,
        today: LocalDate,
        eventDates: Set<LocalDate>
    ) {
        this.month = month
        this.today = today
        this.eventDates = eventDates

        // Resolve màu từ Material Theme (tự động đổi khi dark/light mode)
        colorNormal = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorOnBackground,
            Color.BLACK
        )

        // Tính lại số hàng → cần requestLayout nếu số hàng thay đổi
        val newRowCount = computeRowCount(month)
        if (newRowCount != rowCount) {
            rowCount = newRowCount
            requestLayout()
        } else {
            invalidate()
        }
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        cellSize = if (width > 0) width / 7f else 0f
        textPaint.textSize = cellSize * 0.45f

        // Chiều cao = số hàng × cellSize (wrap_content theo chiều dọc)
        val desiredHeight = (rowCount * cellSize).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cellSize = if (w > 0) w / 7f else 0f
        textPaint.textSize = cellSize * 0.45f
    }

    // ── Drawing ──────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        if (cellSize <= 0f) return

        val firstOffset = getFirstDayOffset(month)   // 0-based, Mon=0
        val daysInMonth = month.lengthOfMonth()

        for (day in 1..daysInMonth) {
            val index = firstOffset + day - 1
            val col = index % 7
            val row = index / 7

            val cx = col * cellSize + cellSize / 2f
            val cy = row * cellSize + cellSize / 2f

            val date = month.atDay(day)
            val isToday = date == today
            val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
            val hasEvent = eventDates.contains(date)

            // Nền tròn hôm nay
            if (isToday) {
                canvas.drawCircle(cx, cy, cellSize * 0.4f, todayPaint)
            }

            // Text màu
            textPaint.color = when {
                isToday -> colorToday
                isSunday -> colorSunday
                else -> colorNormal
            }

            // Vẽ số ngày
            val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(day.toString(), cx, textY, textPaint)

            // Chấm sự kiện (phía dưới số)
            if (hasEvent && !isToday) {
                val dotRadius = (cellSize * 0.07f).coerceAtLeast(2f)
                val dotY = cy + cellSize * 0.36f
                canvas.drawCircle(cx, dotY, dotRadius, dotPaint)
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Offset cột của ngày 1 trong tháng.
     * Tuần bắt đầu Thứ 2 → Mon=0, Tue=1, ..., Sun=6
     */
    private fun getFirstDayOffset(month: YearMonth): Int {
        val dow = month.atDay(1).dayOfWeek  // DayOfWeek.MONDAY = 1 ... SUNDAY = 7
        return (dow.value - 1) % 7          // Mon=0, ..., Sun=6
    }

    /**
     * Số hàng thực tế của tháng (4, 5 hoặc 6).
     */
    private fun computeRowCount(month: YearMonth): Int {
        val offset = getFirstDayOffset(month)
        val totalCells = offset + month.lengthOfMonth()
        return (totalCells + 6) / 7  // ceiling division
    }
}