package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class BasicBuyerAddress(
    @field:XmlElement(name = "BASICBUYERADDRESS")
    var basicBuyerAddress: String? = null,
)
