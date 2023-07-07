package com.ampairs.tally.model

import jakarta.xml.bind.annotation.*

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "ENVELOPE")
@XmlType(propOrder = ["header", "body"])
data class TallyXML(
    @XmlElement(name = "HEADER", type = Header::class)
    var header: Header,

    @XmlElement(name = "BODY", type = Body::class)
    var body: Body

)

fun main() {

}