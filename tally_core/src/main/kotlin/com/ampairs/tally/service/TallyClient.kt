package com.ampairs.tally.service

import com.ampairs.tally.model.TallyXML
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate


const val TALLY_END_POINT = "http://192.168.1.76:9000"

class TallyClient(
    val tallyRestTemplate: RestTemplate
) {
    val headers = HttpHeaders()

    init {
        headers.setContentType(MediaType.TEXT_XML)
    }

    fun post(tallyXML: TallyXML): TallyXML? {
        try {
            val request = HttpEntity<TallyXML>(tallyXML, headers)
            val response = tallyRestTemplate.postForEntity(
                TALLY_END_POINT, request,
                TallyXML::class.java
            )
            return response.body
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

}