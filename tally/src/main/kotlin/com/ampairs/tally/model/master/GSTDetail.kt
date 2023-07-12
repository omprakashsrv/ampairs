package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class GSTDetail(
    @field:XmlElement(name = "APPLICABLEFROM")
    var applicableFrom: String? = null,

    @field:XmlElement(name = "HSNMASTERNAME")
    var hsnMasterName: String? = null,

    @field:XmlElement(name = "HSNCODE")
    var hsnCode: String? = null,

    @field:XmlElement(name = "TAXABILITY")
    var taxability: String? = null,

    @field:XmlElement(name = "ISREVERSECHARGEAPPLICABLE")
    var isReverseChargeApplicable: String? = null,

    @field:XmlElement(name = "ISNONGSTGOODS")
    var isNonGstGoods: String? = null,

    @field:XmlElement(name = "GSTINELIGIBLEITC")
    var gstInEligibleitc: String? = null,

    @field:XmlElement(name = "STATEWISEDETAILS.LIST", type = StateWiseDetail::class)
    var stateWiseDetailsList: List<StateWiseDetail>? = null,
)
