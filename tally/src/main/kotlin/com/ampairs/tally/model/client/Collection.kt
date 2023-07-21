package com.ampairs.tally.model.client

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement


@XmlAccessorType(XmlAccessType.FIELD)
data class Collection(
    @XmlAttribute(name = "ISMODIFY")
    val isModify: String = "No",
    @XmlAttribute(name = "ISFIXED")
    val isFixed: String = "No",
    @XmlAttribute(name = "ISINITIALIZE")
    val isInitialize: String = "No",
    @XmlAttribute(name = "ISOPTION")
    val isOption: String = "No",
    @XmlAttribute(name = "ISINTERNAL")
    val isInternal: String = "No",
    @XmlAttribute(name = "NAME")
    val name: String = "",
    @field:XmlElement(name = "TYPE")
    var type: String = "",
)