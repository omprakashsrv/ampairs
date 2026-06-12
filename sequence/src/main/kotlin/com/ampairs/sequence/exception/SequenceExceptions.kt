package com.ampairs.sequence.exception

class SequenceDefinitionNotFoundException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class SequenceAllocationNotFoundException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** A second active definition for the same (entityType, scope, userId) key. */
class DuplicateSequenceDefinitionException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Generation or block allocation attempted against a deactivated definition. */
class InactiveSequenceDefinitionException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Invalid request semantics — e.g. lowering the counter, USER scope without user_id. */
class InvalidSequenceRequestException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
