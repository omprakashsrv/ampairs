package com.ampairs.product

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.ampairs.product", "com.ampairs.core"])
class TestApplication
