package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class AllBankersDate(
    @XmlAttribute(name = "TYPE")
    var type: String? = null,

    @field:XmlElement(name = "BASICBANKERSDATE")
    var basicBankersDate: String? = null,
)