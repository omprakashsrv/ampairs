package com.ampairs.tally.config

import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource


class JaxbMessageConverter : Jaxb2RootElementHttpMessageConverter() {

    override fun processSource(source: Source): Source {
        val streamSource = source as StreamSource
        val inputStream = streamSource.inputStream
        val replacingInputStream = ReplacingInputStream(inputStream, "&#4;", "")
        streamSource.inputStream = replacingInputStream
        return super.processSource(source)
    }

}