package com.ampairs.core.utils

object Helper {
    fun generateUniqueId(idPrefix: String?, idLength: Int): String {
        return UniqueIdGenerators.ALPHANUMERIC_WITH_TIME_MILLI.generate(
            idPrefix,
            idLength
        )
    }
}
