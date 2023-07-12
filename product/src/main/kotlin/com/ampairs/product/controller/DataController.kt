package com.ampairs.product.controller

import com.ampairs.core.domain.dto.GenericSuccessResponse
import com.ampairs.product.service.ProductService
import com.ampairs.tally.service.TallyService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/product/v1/data")
class DataController constructor(
    val tallyService: TallyService,
    val productService: ProductService
) {

    @PostMapping("/tally/import")
    fun importMasters(@RequestParam("file") file: MultipartFile): GenericSuccessResponse {
        if (file.isEmpty) {
            throw Exception("Failed to read empty file.")
        }

        file.inputStream.use { inputStream ->
            val tallyXML = tallyService.importMasters(inputStream)
            productService.updateTallyXml(tallyXML)
        }
        file.inputStream.close()
        return GenericSuccessResponse()
    }
}