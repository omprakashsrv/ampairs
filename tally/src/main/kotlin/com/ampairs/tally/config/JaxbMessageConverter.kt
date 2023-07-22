package com.ampairs.tally.config

import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource


class JaxbMessageConverter : Jaxb2RootElementHttpMessageConverter() {


    override fun processSource(source: Source): Source {
        val streamSource = source as StreamSource
        val inputStream = streamSource.inputStream
        val replacingInputStream = ReplacingInputStream(inputStream, "&#4;", "")
        val byteArrayOutputStream = ByteArrayOutputStream()
        var length: Int
        while (-1 != replacingInputStream.read().also { length = it }) byteArrayOutputStream.write(length)
        streamSource.inputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        return super.processSource(source)
    }

}