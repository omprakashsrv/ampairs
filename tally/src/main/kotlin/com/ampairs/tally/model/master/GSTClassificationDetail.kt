package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

/**
 * Created by paripranu on 06/13/18.
 */
@XmlAccessorType(XmlAccessType.NONE)
class GSTClassificationDetail {
    @XmlElement(name = "APPLICABLEFROM")
    var applicableFrom: String? = null

    @XmlElement(name = "HSNMASTERNAME")
    var hsnMasterName: String? = null

    @XmlElement(name = "HSNCODE")
    var hsnCode: String? = null

    @XmlElement(name = "TAXABILITY")
    var taxability: String? = null

    @XmlElement(name = "ISREVERSECHARGEAPPLICABLE")
    var isReverseChargeApplicable: String? = null

    @XmlElement(name = "ISNONGSTGOODS")
    var isNonGstGoods: String? = null

    @XmlElement(name = "GSTINELIGIBLEITC")
    var gstInEligibleitc: String? = null

    @XmlElement(name = "STATEWISEDETAILS.LIST", type = StateWiseDetail::class)
    var stateWiseDetailsList: List<StateWiseDetail>? = null
}
