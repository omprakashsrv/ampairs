package com.ampairs.communication.service

/** Aggregate `/sync` optimistic-concurrency conflict — the client must re-pull, re-apply, retry. */
class TemplateVersionConflictException(val code: String, message: String) : RuntimeException(message)

/** A referenced template/variant could not be found for a send or preview. */
class TemplateNotFoundException(message: String) : RuntimeException(message)
