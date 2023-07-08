package com.ampairs.tally.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class Body(
    @field:XmlElement(name = "IMPORTDATA", type = ImportData::class)
    var importData: ImportData? = null
)
