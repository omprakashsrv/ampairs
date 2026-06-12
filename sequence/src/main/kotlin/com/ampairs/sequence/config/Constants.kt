package com.ampairs.sequence.config

object Constants {
    const val SEQUENCE_DEFINITION_PREFIX = "SQD"
    const val SEQUENCE_ALLOCATION_PREFIX = "SQA"

    const val DEFAULT_BLOCK_SIZE = 50
    const val MIN_BLOCK_SIZE = 1
    const val MAX_BLOCK_SIZE = 1000
    const val MAX_PADDING_LENGTH = 18

    /** Sync/event entity type — must match the mobile SyncEntity mapping. */
    const val ENTITY_TYPE_SEQUENCE = "sequence"
}
