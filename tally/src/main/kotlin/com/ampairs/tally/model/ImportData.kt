package com.ampairs.tally.model

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlType

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = ["requestDesc", "requestData"])
data class ImportData(
    @XmlElement(name = "REQUESTDESC", type = RequestDesc::class)
    var requestDesc: RequestDesc,

    @XmlElement(name = "REQUESTDATA", type = RequestData::class)
    var requestData: RequestData
)