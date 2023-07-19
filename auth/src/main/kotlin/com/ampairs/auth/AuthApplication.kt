package com.ampairs.auth

import com.ampairs.core.utils.PropertiesUtils
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class AuthApplication

fun main(args: Array<String>) {
    PropertiesUtils.initProperties()
    runApplication<AuthApplication>(*args)
}
