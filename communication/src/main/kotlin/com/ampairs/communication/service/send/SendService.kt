package com.ampairs.communication.service.send

import com.ampairs.communication.domain.dto.CommunicationRequestResponse
import com.ampairs.communication.domain.dto.SendRequest
import com.ampairs.communication.domain.dto.asResponse
import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.enums.TriggerType
import com.ampairs.communication.port.CustomerAudiencePort
import com.ampairs.communication.port.Recipient
import com.ampairs.communication.repository.CommunicationLogRepository
import com.ampairs.communication.service.TemplateNotFoundException
import com.ampairs.communication.service.template.TemplateService
import org.springframework.stereotype.Service

/** Orchestrates a manual/transactional send: resolve template + audience, then dispatch. */
@Service
class SendService(
    private val templateService: TemplateService,
    private val audiencePort: CustomerAudiencePort,
    private val dispatchService: CommunicationDispatchService,
    private val logRepository: CommunicationLogRepository,
) {

    fun send(request: SendRequest): CommunicationRequestResponse {
        val (template, variants) = templateService.findByCode(request.templateCode)
            ?: throw TemplateNotFoundException("No template with code '${request.templateCode}'")

        val channels = resolveChannels(request.channels, variants.map { it.channel })
        val explicit = request.recipients.map {
            Recipient(it.customerUid, it.email, it.phone, it.pushToken, it.locale)
        }
        val recipients = audiencePort.resolve(request.audienceType, request.audienceRef, explicit)

        val communicationRequest = dispatchService.dispatch(
            template = template,
            variants = variants,
            channels = channels,
            recipients = recipients,
            variables = request.variables,
            triggerType = TriggerType.MANUAL,
            sourceRef = null,
            dedupKey = null,
        )
        val logs = logRepository.findByRequestUid(communicationRequest.uid).map { it.asResponse() }
        return CommunicationRequestResponse(communicationRequest.uid, communicationRequest.status, logs)
    }

    /** Use requested channels if given, else every channel the template has a variant for. */
    private fun resolveChannels(requested: List<String>, variantChannels: List<String>): List<Channel> {
        val source = requested.ifEmpty { variantChannels.distinct() }
        return source.mapNotNull { runCatching { Channel.valueOf(it.uppercase()) }.getOrNull() }.distinct()
    }
}
