package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

/**
 * Created by paripranu on 06/13/18.
 */
@XmlAccessorType(XmlAccessType.NONE)
class AccountingAllocation {
    @XmlElement(name = "LEDGERNAME")
    var ledgerName = ""

    @XmlElement(name = "ISDEEMEDPOSITIVE")
    private var isDeemedPositive = ""

    @XmlElement(name = "ISPARTYLEDGER")
    var isPartyLedger = ""

    @XmlElement(name = "ISLASTDEEMEDPOSITIVE")
    private var isLastDeemedPositive = ""

    @XmlElement(name = "AMOUNT")
    var amount = ""
    fun getIsDeemedPositive(): String {
        return isDeemedPositive
    }

    fun setIsDeemedPositive(isDeemedPositive: String) {
        this.isDeemedPositive = isDeemedPositive
        isLastDeemedPositive = isDeemedPositive
    }

    fun getIsLastDeemedPositive(): String {
        return isLastDeemedPositive
    }

    fun setIsLastDeemedPositive(isLastDeemedPositive: String) {
        this.isLastDeemedPositive = isLastDeemedPositive
        isDeemedPositive = isLastDeemedPositive
    }
}
