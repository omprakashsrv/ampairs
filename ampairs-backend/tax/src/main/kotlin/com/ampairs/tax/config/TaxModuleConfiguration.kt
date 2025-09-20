package com.ampairs.tax.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackages = ["com.ampairs.tax.repository"])
@EntityScan(basePackages = ["com.ampairs.tax.domain.model"])
@EnableTransactionManagement
class TaxModuleConfiguration