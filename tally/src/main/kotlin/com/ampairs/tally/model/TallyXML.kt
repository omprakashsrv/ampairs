package com.ampairs.tally.model

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.annotation.*
import java.io.FileReader
import java.nio.charset.StandardCharsets


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ENVELOPE")
@XmlType(propOrder = arrayOf("header", "body"))
data class TallyXML(
    @XmlAttribute(name = "Action")
    val action: String = "",

    @field:XmlElement(name = "HEADER", type = Header::class)
    var header: Header = Header(),

    @field:XmlElement(name = "BODY", type = Body::class)
    var body: Body = Body()

)

fun main() {
    val context = JAXBContext.newInstance(TallyXML::class.java)
    val fileReader = FileReader("tally.xml",StandardCharsets.UTF_8)
    val marshaller = context.createUnmarshaller()
    marshaller.setEventHandler { it ->
        println("msg : ${it.toString()}")
        return@setEventHandler true
    }

    val unmarshal = marshaller
        .unmarshal(fileReader)
    println("unmarshal = ${unmarshal}")
}