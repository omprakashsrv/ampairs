package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class AccountingAllocation(
    @field:XmlElement(name = "LEDGERNAME")
    var ledgerName: String? = null,

    @field:XmlElement(name = "ISDEEMEDPOSITIVE")
    private var isDeemedPositive: String? = null,

    @field:XmlElement(name = "ISPARTYLEDGER")
    var isPartyLedger: String? = null,

    @field:XmlElement(name = "ISLASTDEEMEDPOSITIVE")
    private var isLastDeemedPositive: String? = null,

    @field:XmlElement(name = "AMOUNT")
    var amount: String? = null,
)
