package com.ampairs.invoice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.ampairs.invoice", "com.ampairs.core"])
class TestApplication
