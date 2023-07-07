package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class BasicBuyerAddress {
    @XmlElement(name = "BASICBUYERADDRESS")
    var basicBuyerAddress: String? = null

    constructor()
    constructor(basicBuyerAddress: String?) {
        this.basicBuyerAddress = basicBuyerAddress
    }
}
