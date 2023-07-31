package com.ampairs.core.multitenancy

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.stereotype.Component
import javax.sql.DataSource


@Component
class MultitenantDataSource @Autowired constructor(val tenantIdentifierResolver: TenantIdentifierResolver) :
    AbstractRoutingDataSource() {

    init {
        this.setDefaultTargetDataSource(createDataSource("munsi_app"))
        val targetDataSources = HashMap<Any, Any>()
        targetDataSources["CMP12345"] = createDataSource("CMP12345")
        this.setTargetDataSources(targetDataSources)
    }

    override fun determineCurrentLookupKey(): String? {
        return tenantIdentifierResolver.resolveCurrentTenantIdentifier()
    }

    private fun createDataSource(name: String): DataSource {
        val factory = DataSourceBuilder
            .create().driverClassName("com.mysql.cj.jdbc.Driver")
            .username("root")
            .password("pass")
            .url("jdbc:mysql://localhost:3306/$name")
        return factory.build()
    }
}