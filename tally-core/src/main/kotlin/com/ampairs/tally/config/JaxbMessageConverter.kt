package com.ampairs.tally.config

import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource


class JaxbMessageConverter : Jaxb2RootElementHttpMessageConverter() {

    override fun processSource(source: Source): Source {
        val streamSource = source as StreamSource
        streamSource.inputStream = com.ampairs.tally.config.ReplacingInputStream(streamSource.inputStream, "&#4;", "")
        return super.processSource(source)
    }

}