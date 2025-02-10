package com.ampairs

import com.ampairs.core.utils.PropertiesUtils
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching


@SpringBootApplication
@EnableCaching
class AmpairsApplication

fun main(args: Array<String>) {
    PropertiesUtils.initProperties()
    runApplication<AmpairsApplication>(*args)
}
