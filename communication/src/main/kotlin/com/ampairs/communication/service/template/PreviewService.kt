package com.ampairs.communication.service.template

import com.ampairs.communication.domain.dto.PreviewResponse
import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.model.TemplateVariant
import com.ampairs.communication.service.TemplateNotFoundException
import org.springframework.stereotype.Service

/** Renders a template variant with sample data (FR-006): substituted output + missing-placeholder list. */
@Service
class PreviewService(
    private val templateService: TemplateService,
    private val renderer: TemplateRenderer,
) {
    fun preview(code: String, channel: String, locale: String?, variables: Map<String, String>): PreviewResponse {
        val (template, variants) = templateService.findByCode(code)
            ?: throw TemplateNotFoundException("No template with code '$code'")
        val ch = runCatching { Channel.valueOf(channel.uppercase()) }
            .getOrElse { throw TemplateNotFoundException("Unknown channel '$channel'") }
        val wantLocale = locale ?: template.defaultLocale
        val variant = pick(variants, ch, wantLocale, template.defaultLocale)
            ?: throw TemplateNotFoundException("No $channel variant for template '$code'")

        val missing = LinkedHashSet<String>()
        val subject = variant.subject?.let { renderer.render(it, variables).also { r -> missing += r.missingVariables }.output }
        val html = variant.htmlBody?.let { renderer.render(it, variables).also { r -> missing += r.missingVariables }.output }
        val text = when {
            !variant.textBody.isNullOrBlank() ->
                renderer.render(variant.textBody, variables).also { missing += it.missingVariables }.output
            html != null -> renderer.htmlToPlainText(html)
            else -> ""
        }
        return PreviewResponse(subject, html, text, missing.toList())
    }

    private fun pick(variants: List<TemplateVariant>, channel: Channel, locale: String, default: String): TemplateVariant? {
        val forChannel = variants.filter { it.channel.equals(channel.name, ignoreCase = true) }
        return forChannel.firstOrNull { it.locale.equals(locale, ignoreCase = true) }
            ?: forChannel.firstOrNull { it.locale.equals(default, ignoreCase = true) }
            ?: forChannel.firstOrNull()
    }
}
