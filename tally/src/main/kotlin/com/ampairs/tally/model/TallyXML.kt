package com.ampairs.tally.model

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.annotation.*
import java.io.FileReader


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ENVELOPE")
@XmlType(propOrder = arrayOf("header", "body"))
data class TallyXML(
    @field:XmlElement(name = "HEADER", type = Header::class)
    var header: Header? = null,

    @field:XmlElement(name = "BODY", type = Body::class)
    var body: Body? = null

)

fun main() {
    val context = JAXBContext.newInstance(TallyXML::class.java)
    val fileReader = FileReader("tally.xml")
//    val readText = fileReader.readText()
//    println("readText = ${readText}")
    val unmarshal = context.createUnmarshaller()
        .unmarshal(fileReader)
    println("unmarshal = ${unmarshal}")
}