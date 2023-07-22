package com.ampairs.tally.model.client

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement


@XmlAccessorType(XmlAccessType.FIELD)
data class Desc(
    @field:XmlElement(name = "TDL")
    var tdl: TDL = TDL(),
)