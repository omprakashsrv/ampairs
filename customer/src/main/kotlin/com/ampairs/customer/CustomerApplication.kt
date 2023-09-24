package com.ampairs.customer

import com.ampairs.core.utils.PropertiesUtils
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CustomerApplication

fun main(args: Array<String>) {
	PropertiesUtils.initProperties()
	runApplication<CustomerApplication>(*args)
}
