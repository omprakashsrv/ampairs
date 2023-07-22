package com.ampairs.tally.model.voucher

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute

@XmlAccessorType(XmlAccessType.FIELD)
data class BankAllocation(
    @XmlAttribute(name = "DATE")
    var date: String? = null,

    @XmlAttribute(name = "INSTRUMENTDATE")
    var instrumentDate: String? = null,

    @XmlAttribute(name = "TRANSACTIONTYPE")
    var transactionType: String? = null,

    @XmlAttribute(name = "BANKNAME")
    var bankName: String? = null,

    @XmlAttribute(name = "PAYMENTFAVOURING")
    var paymentFavouring: String? = null,

    @XmlAttribute(name = "INSTRUMENTNUMBER")
    var instrumentNumber: String? = null,

    @XmlAttribute(name = "UNIQUEREFERENCENUMBER")
    var uniqueReferenceNumber: String? = null,

    @XmlAttribute(name = "PAYMENTMODE")
    var paymentMode: String? = null,

    @XmlAttribute(name = "STATUS")
    var status: String? = null,

    @XmlAttribute(name = "BANKPARTYNAME")
    var bankPartyName: String? = null,

    @XmlAttribute(name = "AMOUNT")
    var amount: String? = null,
)
