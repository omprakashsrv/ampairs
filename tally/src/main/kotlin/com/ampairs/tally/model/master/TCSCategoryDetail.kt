package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class TCSCategoryDetail {
    @XmlElement(name = "CATEGORYDATE")
    var categoryDate: String? = null

    @XmlElement(name = "CATEGORYNAME")
    var categoryName: String? = null
}
