package com.ampairs.workspace

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.ampairs.workspace", "com.ampairs.core"])
class TestApplication
