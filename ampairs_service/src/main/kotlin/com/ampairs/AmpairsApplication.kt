package com.ampairs

import com.ampairs.core.utils.PropertiesUtils
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class AmpairsApplication

fun main(args: Array<String>) {
    PropertiesUtils.initProperties()
    runApplication<AmpairsApplication>(*args)
}
