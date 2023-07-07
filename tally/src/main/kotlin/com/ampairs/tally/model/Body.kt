package com.ampairs.tally.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
data class Body(
    @XmlElement(name = "IMPORTDATA", type = ImportData::class)
    var importData: ImportData
)
