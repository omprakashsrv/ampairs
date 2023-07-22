package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class TCSCategoryDetail(
    @field:XmlElement(name = "CATEGORYDATE")
    var categoryDate: String? = null,

    @field:XmlElement(name = "CATEGORYNAME")
    var categoryName: String? = null,
)
