package com.ampairs.order

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.ampairs.order", "com.ampairs.core"])
class TestApplication
