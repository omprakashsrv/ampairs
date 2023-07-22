package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class Address(
    @field:XmlElement(name = "ADDRESS")
    var address: String? = null
) {
    override fun toString(): String {
        return "$address"
    }
}
