package com.ampairs.messagebroker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MessageBrokerApplication

fun main(args: Array<String>) {
    runApplication<MessageBrokerApplication>(*args)
}
