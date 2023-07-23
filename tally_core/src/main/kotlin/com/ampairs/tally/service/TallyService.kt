package com.ampairs.tally.service

import com.ampairs.tally.model.TallyXML
import jakarta.xml.bind.JAXBContext
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class TallyService {
    fun importMasters(inputStream: InputStream): TallyXML? {
        val context = JAXBContext.newInstance(TallyXML::class.java)
        return context.createUnmarshaller().unmarshal(inputStream) as TallyXML?
    }
}