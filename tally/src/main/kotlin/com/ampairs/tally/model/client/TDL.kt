package com.ampairs.tally.model.client

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class TDL(
    @field:XmlElement(name = "TDLMESSAGE")
    var tdlMessage: TDLMessage = TDLMessage(),
)