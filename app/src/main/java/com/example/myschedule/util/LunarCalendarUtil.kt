package com.example.myschedule.util

import java.time.LocalDate
import kotlin.math.sin

/**
 * Chuyển đổi dương lịch → âm lịch Việt Nam.
 * Thuật toán dựa trên công thức của Ho Ngoc Duc (2004).
 * Múi giờ: UTC+7 (Việt Nam).
 */
object LunarCalendarUtil {

    private const val TIME_ZONE_OFFSET = 7.0 // UTC+7

    /**
     * Trả về chuỗi ngày âm lịch ngắn gọn, ví dụ: "15/4" hoặc "1/1"
     * Nếu là ngày đầu tháng âm → trả về "1/[tháng]" để dễ nhận biết
     */
    fun toLunarDateShort(date: LocalDate): String {
        val lunar = convertSolarToLunar(date.dayOfMonth, date.monthValue, date.year)
        val lunarDay = lunar[0]
        val lunarMonth = lunar[1]
        return "$lunarDay/$lunarMonth"
    }

    /**
     * Trả về [lunarDay, lunarMonth, lunarYear, isLeapMonth]
     */
    fun convertSolarToLunar(solarDay: Int, solarMonth: Int, solarYear: Int): IntArray {
        val dayNumber = jdFromDate(solarDay, solarMonth, solarYear)
        val k = ((dayNumber - 2415021.076998695) / 29.530588853).toInt()
        var monthStart = getNewMoonDay(k + 1, TIME_ZONE_OFFSET)
        if (monthStart > dayNumber) monthStart = getNewMoonDay(k, TIME_ZONE_OFFSET)

        var a11 = getLunarMonth11(solarYear, TIME_ZONE_OFFSET)
        var b11 = a11
        val lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = solarYear
            a11 = getLunarMonth11(solarYear - 1, TIME_ZONE_OFFSET)
        } else {
            lunarYear = solarYear + 1
            b11 = getLunarMonth11(solarYear + 1, TIME_ZONE_OFFSET)
        }

        val lunarDay = dayNumber - monthStart + 1
        val diff = ((monthStart - a11) / 29)
        var lunarLeap = false
        var lunarMonth = diff + 11

        if (b11 - a11 > 365) {
            val leapMonthDiff = getLeapMonthOffset(a11, TIME_ZONE_OFFSET)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) lunarLeap = true
            }
        }

        if (lunarMonth > 12) lunarMonth -= 12
        if (lunarMonth >= 11 && diff < 4) lunarYear - 1

        return intArrayOf(lunarDay, lunarMonth, lunarYear, if (lunarLeap) 1 else 0)
    }

    private fun jdFromDate(dd: Int, mm: Int, yy: Int): Int {
        val a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        var jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161) {
            jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083
        }
        return jd
    }

    private fun getNewMoonDay(k: Int, timeZone: Double): Int {
        val T = k / 1236.85
        val T2 = T * T
        val T3 = T2 * T
        val dr = Math.PI / 180.0

        var Jd1 = 2415020.75933 + 29.53058868 * k
        Jd1 += 0.0001178 * T2 - 0.000000155 * T3
        Jd1 += 0.00033 * sin((166.56 + 132.87 * T - 0.009173 * T2) * dr)

        val M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3
        val Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3
        val F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3

        var C1 = (0.1734 - 0.000393 * T) * sin(M * dr)
        C1 += 0.0021 * sin(2 * dr * M)
        C1 -= 0.4068 * sin(Mpr * dr)
        C1 += 0.0161 * sin(2 * dr * Mpr)
        C1 -= 0.0004 * sin(3 * dr * Mpr)
        C1 += 0.0104 * sin(2 * dr * F)
        C1 -= 0.0051 * sin((M + Mpr) * dr)
        C1 -= 0.0074 * sin((M - Mpr) * dr)
        C1 += 0.0004 * sin((2 * F + M) * dr)
        C1 -= 0.0004 * sin((2 * F - M) * dr)
        C1 -= 0.0006 * sin((2 * F + Mpr) * dr)
        C1 += 0.0010 * sin((2 * F - Mpr) * dr)
        C1 += 0.0005 * sin((M + 2 * Mpr) * dr)

        val deltat = if (T < -11) {
            0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3
        } else {
            -0.000278 + 0.000265 * T + 0.000262 * T2
        }

        val JdNew = Jd1 + C1 - deltat
        return (JdNew + 0.5 + timeZone / 24).toInt()
    }

    private fun getSunLongitude(jdn: Int, timeZone: Double): Int {
        val T = (jdn - 2451545.5 - timeZone / 24) / 36525
        val T2 = T * T
        val dr = Math.PI / 180
        val M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2
        val L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2
        var DL = (1.9146 - 0.004817 * T - 0.000014 * T2) * sin(dr * M)
        DL += (0.019993 - 0.000101 * T) * sin(dr * 2 * M)
        DL += 0.00029 * sin(dr * 3 * M)
        var L = L0 + DL
        L /= 360.0
        L -= L.toInt()
        return (L * 12).toInt()
    }

    private fun getLunarMonth11(yy: Int, timeZone: Double): Int {
        val off = jdFromDate(31, 12, yy) - 2415021
        val k = (off / 29.530588853).toInt()
        var nm = getNewMoonDay(k, timeZone)
        val sunLong = getSunLongitude(nm, timeZone)
        if (sunLong >= 9) nm = getNewMoonDay(k - 1, timeZone)
        return nm
    }

    private fun getLeapMonthOffset(a11: Int, timeZone: Double): Int {
        val k = ((a11 - 2415021.076998695) / 29.530588853 + 0.5).toInt()
        var last: Int
        var i = 1
        var arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        do {
            last = arc
            i++
            arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        } while (arc != last && i < 14)
        return i - 1
    }
}