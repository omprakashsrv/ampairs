package com.ampairs.tally.config

import com.ampairs.tally.service.TallyClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter
import org.springframework.web.client.RestTemplate


@Configuration
class ApplicationConfig @Autowired constructor() {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        val converter = Jaxb2RootElementHttpMessageConverter()
        converter.supportedMediaTypes = mutableListOf(MediaType.APPLICATION_XML)
        return builder.messageConverters(converter).build()
    }

    @Bean
    fun jaxb2RootElementHttpMessageConverter(): Jaxb2RootElementHttpMessageConverter? {
        val converter = Jaxb2RootElementHttpMessageConverter()
        converter.isSupportDtd = true
        return converter
    }

    @Bean
    fun tallyClient(restTemplate: RestTemplate): TallyClient {
        return TallyClient(restTemplate)
    }

}
