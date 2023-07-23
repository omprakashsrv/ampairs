package com.ampairs.tally.config

import com.ampairs.tally.service.TallyClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter
import org.springframework.web.client.RestTemplate


@Configuration
class TallyConfig @Autowired constructor() {

    @Bean
    fun tallyRestTemplate(
        tallyRestTemplateBuilder: RestTemplateBuilder, loggingInterceptor: ClientHttpRequestInterceptor
    ): RestTemplate {
        val converter = JaxbMessageConverter()
        converter.supportedMediaTypes = mutableListOf(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
        return tallyRestTemplateBuilder.messageConverters(converter).interceptors(loggingInterceptor).build()
    }

    @Bean
    fun tallyRestTemplateBuilder(): RestTemplateBuilder {
        return RestTemplateBuilder()
    }

    @Bean
    fun loggingInterceptor(): ClientHttpRequestInterceptor {
        return LoggingInterceptor()
    }

    @Bean
    fun jaxb2RootElementHttpMessageConverter(): Jaxb2RootElementHttpMessageConverter? {
        val converter = Jaxb2RootElementHttpMessageConverter()
        converter.isSupportDtd = true
        return converter
    }

    @Bean
    fun tallyClient(tallyRestTemplate: RestTemplate): TallyClient {
        return TallyClient(tallyRestTemplate)
    }

}
