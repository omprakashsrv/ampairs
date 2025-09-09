package com.ampairs.customer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.ampairs.customer", "com.ampairs.core"])
class TestApplication