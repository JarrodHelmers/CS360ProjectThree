package com.example.weight_trackerapp.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

/** Parse "yyyy-MM-dd" → LocalDate (safe) */
fun parseDate(yyyyMmDd: String): LocalDate =
    LocalDate.parse(yyyyMmDd, DATE_FMT)

/** Running statistics */
data class RunningStats(
    val count: Int,
    val sum: Double,
    val min: Double?,
    val max: Double?
) {
    val avg: Double? get() = if (count > 0) sum / count else null
}

/** all-time stats from a list. */
fun computeStats(weights: List<Double>): RunningStats {
    if (weights.isEmpty()) return RunningStats(0, 0.0, null, null)
    var sum = 0.0
    var min = Double.POSITIVE_INFINITY
    var max = Double.NEGATIVE_INFINITY
    for (w in weights) {
        sum += w
        if (w < min) min = w
        if (w > max) max = w
    }
    return RunningStats(weights.size, sum, min, max)
}

/** Average for the last [days] days based on ISO date strings (yyyy-MM-dd). */
fun rollingAverage(
    entries: List<Pair<String, Double>>,
    days: Long
): Double? {
    if (entries.isEmpty()) return null
    val today = LocalDate.now()
    val cutoff = today.minusDays(days - 1) // include today as day 1
    var sum = 0.0
    var n = 0
    for ((dateStr, kg) in entries) {
        val d = parseDate(dateStr)
        if (!d.isBefore(cutoff)) {
            sum += kg
            n++
        }
    }
    return if (n > 0) sum / n else null
}

/** Returns slope per day. Positive = up, negative = down, ~0 = flat. */
fun trendSlope(
    entries: List<Pair<String, Double>>,
    window: Int = 14
): Double? {
    if (entries.size < 2) return null
    // Use most recent 'window' items in chronological order
    val slice = entries
        .take(window)
        .sortedBy { parseDate(it.first) }

    if (slice.size < 2) return null

    // x = 0..n-1 (days increasing), y = weight
    val n = slice.size
    val ys = slice.map { it.second }

    val meanX = (n - 1) / 2.0
    val meanY = ys.average()

    var num = 0.0
    var den = 0.0
    for (i in 0 until n) {
        val dx = i - meanX
        val dy = ys[i] - meanY
        num += dx * dy
        den += dx * dx
    }
    return if (abs(den) < 1e-9) null else num / den
}

/** arrow for slope with a tiny dead-zone. */
fun trendArrow(slope: Double?, flatEpsilon: Double = 0.002): String {
    slope ?: return "—"
    return when {
        slope > flatEpsilon -> "↑"
        slope < -flatEpsilon -> "↓"
        else -> "→"
    }
}
