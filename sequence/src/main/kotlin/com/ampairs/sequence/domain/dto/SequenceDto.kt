package com.ampairs.sequence.domain.dto

import com.ampairs.sequence.domain.model.AllocationStatus
import com.ampairs.sequence.domain.model.SequenceAllocation
import com.ampairs.sequence.domain.model.SequenceDefinition
import com.ampairs.sequence.domain.model.SequenceScope
import com.ampairs.sequence.service.SequenceFormatter
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class SequenceDefinitionRequest(
    val uid: String? = null,
    @field:NotBlank
    @field:Size(max = 64)
    val entityType: String = "",
    val scope: SequenceScope = SequenceScope.WORKSPACE,
    val userId: String? = null,
    @field:Size(max = 32)
    val prefix: String? = null,
    @field:Size(max = 32)
    val suffix: String? = null,
    @field:Min(0)
    @field:Max(18)
    val paddingLength: Int = 0,
    @field:Min(1)
    val startValue: Long = 1,
    @field:Min(1)
    val incrementStep: Int = 1,
    /**
     * Optional counter fast-forward. Ignored when lower than the server's current value
     * on bulk sync upserts; rejected on direct create/update (counter never moves backwards).
     */
    @field:Min(0)
    val currentValue: Long? = null,
    val active: Boolean = true,
    val refId: String? = null,
)

data class SequenceDefinitionResponse(
    val uid: String,
    val entityType: String,
    val scope: SequenceScope,
    val userId: String?,
    val prefix: String?,
    val suffix: String?,
    val paddingLength: Int,
    val startValue: Long,
    val incrementStep: Int,
    val currentValue: Long,
    /** Formatted next number that would be issued (informational; nothing is reserved). */
    val nextPreview: String,
    val active: Boolean,
    val refId: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

data class SequenceNextRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val entityType: String = "",
)

data class SequencePreviewResponse(
    val definitionUid: String,
    val nextValue: Long,
    val formatted: String,
)

data class SequenceNumberResponse(
    val definitionUid: String,
    val entityType: String,
    val value: Long,
    val formatted: String,
)

data class SequenceAllocationRequest(
    @field:NotBlank
    @field:Size(max = 64)
    val entityType: String = "",
    @field:NotBlank
    @field:Size(max = 200)
    val deviceId: String = "",
    @field:Min(1)
    @field:Max(1000)
    val blockSize: Int = 50,
)

data class SequenceAllocationReportRequest(
    @field:NotBlank
    val uid: String = "",
    @field:Min(0)
    val nextAvailable: Long = 0,
)

data class SequenceAllocationResponse(
    val uid: String,
    val definitionUid: String,
    val entityType: String,
    val deviceId: String,
    val rangeStart: Long,
    val rangeEnd: Long,
    val nextAvailable: Long,
    val status: AllocationStatus,
    val prefix: String?,
    val suffix: String?,
    val paddingLength: Int,
    val incrementStep: Int,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun SequenceDefinition.asSequenceDefinitionResponse(): SequenceDefinitionResponse = SequenceDefinitionResponse(
    uid = uid,
    entityType = entityType,
    scope = scope,
    userId = userId,
    prefix = prefix,
    suffix = suffix,
    paddingLength = paddingLength,
    startValue = startValue,
    incrementStep = incrementStep,
    currentValue = currentValue,
    nextPreview = SequenceFormatter.format(
        prefix = prefix,
        suffix = suffix,
        paddingLength = paddingLength,
        value = SequenceFormatter.nextValue(currentValue, startValue, incrementStep),
    ),
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SequenceAllocation.asSequenceAllocationResponse(): SequenceAllocationResponse = SequenceAllocationResponse(
    uid = uid,
    definitionUid = definitionUid,
    entityType = entityType,
    deviceId = deviceId,
    rangeStart = rangeStart,
    rangeEnd = rangeEnd,
    nextAvailable = nextAvailable,
    status = status,
    prefix = prefix,
    suffix = suffix,
    paddingLength = paddingLength,
    incrementStep = incrementStep,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
