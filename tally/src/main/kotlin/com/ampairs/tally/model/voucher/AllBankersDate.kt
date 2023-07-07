package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class AllBankersDate {
    @XmlAttribute(name = "TYPE")
    var type = "Date"

    @XmlElement(name = "BASICBANKERSDATE")
    var basicBankersDate: String? = null
}