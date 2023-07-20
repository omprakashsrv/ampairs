package com.ampairs.tally.model.export

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class Body(
    @field:XmlElement(name = "DESC", type = Desc::class)
    var desc: Desc = Desc()
)