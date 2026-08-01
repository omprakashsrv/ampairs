package com.ampairs.payment.service

/**
 * Aging bucket boundaries, parsed from the `aging_buckets` workspace setting
 * (comma-separated upper day-bounds, default `30,60,90`). Produces labels like
 * `0-30`, `31-60`, `61-90`, `90+` and classifies a days-overdue value into one.
 */
class AgingBuckets private constructor(val bounds: List<Int>) {

    /** Ordered bucket labels (last is the open-ended `<lastBound>+`). */
    fun labels(): List<String> {
        if (bounds.isEmpty()) return listOf("0+")
        val result = mutableListOf<String>()
        var lower = 0
        for (b in bounds) {
            result.add("$lower-$b")
            lower = b + 1
        }
        result.add("${bounds.last()}+")
        return result
    }

    /** Bucket label for a days-overdue value (negative/zero ⇒ first bucket). */
    fun classify(daysOverdue: Long): String {
        val labels = labels()
        if (bounds.isEmpty()) return labels.first()
        val d = if (daysOverdue < 0) 0 else daysOverdue
        for ((i, b) in bounds.withIndex()) {
            if (d <= b) return labels[i]
        }
        return labels.last()
    }

    companion object {
        val DEFAULT = AgingBuckets(listOf(30, 60, 90))

        fun parse(raw: String?): AgingBuckets {
            if (raw.isNullOrBlank()) return DEFAULT
            val bounds = raw.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it > 0 }
                .distinct()
                .sorted()
            return if (bounds.isEmpty()) DEFAULT else AgingBuckets(bounds)
        }
    }
}
