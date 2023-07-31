package com.ampairs.core.multitenancy

import org.hibernate.cfg.AvailableSettings
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource


@Component
class ConnectionProvider @Autowired constructor(val dataSource: DataSource) : MultiTenantConnectionProvider,
    HibernatePropertiesCustomizer {
    override fun isUnwrappableAs(unwrapType: Class<*>?): Boolean {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> unwrap(unwrapType: Class<T>?): T {
        TODO("Not yet implemented")
    }


    @Throws(SQLException::class)
    override fun getAnyConnection(): Connection? {
        return dataSource.connection
    }

    @Throws(SQLException::class)
    override fun releaseAnyConnection(connection: Connection) {
        connection.close()
    }

    @Throws(SQLException::class)
    override fun getConnection(schema: String?): Connection? {
        val connection = dataSource.connection
        connection.schema  = schema
        return connection
    }

    @Throws(SQLException::class)
    override fun releaseConnection(schema: String?, connection: Connection) {
        connection.close()
    }

    override fun supportsAggressiveRelease(): Boolean {
        return true
    }

    override fun customize(hibernateProperties: MutableMap<String?, Any?>) {
        hibernateProperties[AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER] = this
    }

}