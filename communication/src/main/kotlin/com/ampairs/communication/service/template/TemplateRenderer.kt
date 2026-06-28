package com.ampairs.communication.service.template

import org.springframework.stereotype.Component

/** Result of rendering a single string: the output plus any placeholders that had no value. */
data class RenderResult(val output: String, val missingVariables: List<String>)

/** Thrown when a required placeholder has no value during a real (non-preview) render. */
class TemplateRenderException(message: String) : RuntimeException(message)

/**
 * Logic-less Mustache-style renderer. Substitutes `{{ variable }}` tokens from a flat context map.
 * NO loops, conditionals, or expressions — this eliminates server-side template-injection risk;
 * business users author content and only a fixed variable context is interpolated.
 */
@Component
class TemplateRenderer {

    private val tokenRegex = Regex("""\{\{\s*([a-zA-Z0-9_.]+)\s*}}""")

    /** Render a template string, collecting (not failing on) missing placeholders. */
    fun render(template: String?, variables: Map<String, String?>): RenderResult {
        if (template.isNullOrEmpty()) return RenderResult(template ?: "", emptyList())
        val missing = LinkedHashSet<String>()
        val output = tokenRegex.replace(template) { match ->
            val key = match.groupValues[1]
            val value = variables[key]
            if (value == null) {
                missing.add(key)
                ""
            } else {
                value
            }
        }
        return RenderResult(output, missing.toList())
    }

    /**
     * Render for a real send: throws [TemplateRenderException] if any placeholder is unresolved,
     * so a blank/half-filled message is never dispatched.
     */
    fun renderRequired(template: String?, variables: Map<String, String?>): String {
        val result = render(template, variables)
        if (result.missingVariables.isNotEmpty()) {
            throw TemplateRenderException(
                "Unresolved template variables: ${result.missingVariables.joinToString(", ")}"
            )
        }
        return result.output
    }

    /**
     * Derive a reasonable plain-text alternative from an HTML body (used when an email variant has
     * no explicit text_body). Strips tags, collapses whitespace, decodes a few common entities.
     */
    fun htmlToPlainText(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return html
            .replace(Regex("(?is)<(script|style).*?</\\1>"), "")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}
