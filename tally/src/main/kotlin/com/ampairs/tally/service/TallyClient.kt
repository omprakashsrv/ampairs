package com.ampairs.tally.service

import com.ampairs.tally.model.TallyXML
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets


const val TALLY_END_POINT = "http://192.168.1.76:9000"

class TallyClient @Autowired constructor(
    val restTemplate: RestTemplate
) {
    val headers = HttpHeaders()

    init {
        headers.setContentType(MediaType.TEXT_XML)
        val mediaType = MediaType(MediaType.TEXT_XML, StandardCharsets.UTF_16)
        headers.accept = mutableListOf(mediaType)
    }

    fun post(tallyXML: TallyXML): ResponseEntity<TallyXML>? {
        try {
            val request = HttpEntity<TallyXML>(tallyXML, headers)
            val response = restTemplate.postForEntity(
                TALLY_END_POINT, request,
                TallyXML::class.java
            )
            return response
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

}