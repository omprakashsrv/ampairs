package com.ampairs.tally.model

import com.ampairs.tally.model.client.Collection
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class Data(
    @field:XmlElement(name = "COLLECTION", type = Collection::class)
    var collection: Collection? = null
)