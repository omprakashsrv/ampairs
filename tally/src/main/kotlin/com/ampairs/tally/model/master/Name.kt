package com.ampairs.tally.model.master

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.NONE)
class Name {
    @XmlElement(name = "NAME")
    var name: String? = null

    constructor()
    constructor(name: String?) {
        this.name = name
    }
}
