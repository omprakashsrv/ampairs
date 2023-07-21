package com.ampairs.tally.service

import com.ampairs.tally.model.TallyXML
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate


const val TALLY_END_POINT = "http://localhost:9000"

class TallyClient @Autowired constructor(
    val restTemplate: RestTemplate
) {
    val headers = HttpHeaders()

    init {
        headers.setContentType(MediaType.APPLICATION_XML);
    }

    fun post(envelope: TallyXML): ResponseEntity<String>? {
        try {
            val request = HttpEntity<TallyXML>(envelope, headers)
            val response = restTemplate.postForEntity(
                TALLY_END_POINT, request,
                String::class.java
            )
            println("response = ${response}")
            return response
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

}