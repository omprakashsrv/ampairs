package com.ampairs.trade.exception

/** Generic trade validation failure → HTTP 422. */
class TradeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** A cross-tenant read was attempted without a sufficiently-scoped ACCEPTED link → HTTP 403. */
class ConsentRequiredException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** An illegal trade-link state transition was attempted → HTTP 409. */
class LinkStateException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
