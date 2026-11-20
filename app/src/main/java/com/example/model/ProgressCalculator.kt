package com.example.model

data class ProgressMetrics(
    val readyCount: Int,
    val inProgressCount: Int,
    val notStartedCount: Int,
    val totalCount: Int,
    val readyWeight: Int,
    val totalWeight: Int,
    val wordWeightedFraction: Float,
    val countFraction: Float
) {
    fun fractionFor(mode: ProgressMode): Float = when (mode) {
        ProgressMode.WORD_WEIGHTED -> wordWeightedFraction
        ProgressMode.ESSAY_COUNT -> countFraction
    }

    fun percentFor(mode: ProgressMode): Int = (fractionFor(mode) * 100f).toInt().coerceIn(0, 100)
}

object ProgressCalculator {
    fun compute(supplements: List<Supplement>): ProgressMetrics {
        if (supplements.isEmpty()) {
            return ProgressMetrics(
                readyCount = 0,
                inProgressCount = 0,
                notStartedCount = 0,
                totalCount = 0,
                readyWeight = 0,
                totalWeight = 0,
                wordWeightedFraction = 0f,
                countFraction = 0f
            )
        }
        var readyCount = 0
        var inProgressCount = 0
        var notStartedCount = 0
        var readyWeight = 0
        var totalWeight = 0

        for (supp in supplements) {
            val weight = supp.wordBucketEnum.weight
            totalWeight += weight
            when (supp.statusEnum) {
                SupplementStatus.READY_TO_SUBMIT -> {
                    readyCount++
                    readyWeight += weight
                }
                SupplementStatus.IN_PROGRESS -> inProgressCount++
                SupplementStatus.NOT_STARTED -> notStartedCount++
            }
        }

        val totalCount = supplements.size
        val wordWeightedFraction = if (totalWeight > 0) readyWeight.toFloat() / totalWeight else 0f
        val countFraction = if (totalCount > 0) readyCount.toFloat() / totalCount else 0f

        return ProgressMetrics(
            readyCount = readyCount,
            inProgressCount = inProgressCount,
            notStartedCount = notStartedCount,
            totalCount = totalCount,
            readyWeight = readyWeight,
            totalWeight = totalWeight,
            wordWeightedFraction = wordWeightedFraction.coerceIn(0f, 1f),
            countFraction = countFraction.coerceIn(0f, 1f)
        )
    }
}
