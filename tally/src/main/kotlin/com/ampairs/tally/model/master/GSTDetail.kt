package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class GSTDetail(
    @field:XmlElement(name = "APPLICABLEFROM")
    var applicableFrom: String? = null,

    @field:XmlElement(name = "HSNCODE")
    var hsnCode: String? = null,

    @field:XmlElement(name = "HSN")
    var hsn: String? = null,

    @field:XmlElement(name = "TAXABILITY")
    var taxability: String? = null,

    @field:XmlElement(name = "STATEWISEDETAILS.LIST", type = StateWiseDetail::class)
    var stateWiseDetailsList: List<StateWiseDetail>? = null,
)
