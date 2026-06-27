package com.ampairs.communication.controller

import com.ampairs.communication.domain.dto.CommunicationRequestResponse
import com.ampairs.communication.domain.dto.SendRequest
import com.ampairs.communication.service.send.SendService
import com.ampairs.core.domain.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Manual/transactional send: renders a template and dispatches to the resolved audience. */
@RestController
@RequestMapping("/communication/v1/requests")
@Validated
class CommunicationRequestController(
    private val sendService: SendService,
) {

    @PostMapping
    fun send(@RequestBody @Valid request: SendRequest): ApiResponse<CommunicationRequestResponse> =
        ApiResponse.success(sendService.send(request))
}
