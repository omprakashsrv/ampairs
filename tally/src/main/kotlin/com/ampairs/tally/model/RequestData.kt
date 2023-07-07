package com.ampairs.tally.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "REQUESTDATA")
data class RequestData(
    @XmlElement(name = "TALLYMESSAGE", type = TallyMessage::class)
    var tallyMessage: MutableList<TallyMessage?>
)
