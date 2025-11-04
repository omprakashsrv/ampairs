package com.ampairs.workspace

import com.ampairs.workspace.model.enums.ModuleStatus
import com.ampairs.workspace.model.enums.WorkspaceModuleStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest(classes = [com.ampairs.AmpairsApplication::class])
@TestPropertySource(properties = [
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
class ModuleStatusSerializationTest {

    @Test
    fun `ModuleStatus should serialize to enum name not displayName`() {
        val objectMapper = ObjectMapper()
        
        val result = objectMapper.writeValueAsString(ModuleStatus.ACTIVE)
        assertEquals("\"ACTIVE\"", result)
        
        val deprecatedResult = objectMapper.writeValueAsString(ModuleStatus.DEPRECATED)
        assertEquals("\"DEPRECATED\"", deprecatedResult)
    }
    
    @Test
    fun `WorkspaceModuleStatus should serialize to enum name not displayName`() {
        val objectMapper = ObjectMapper()
        
        val result = objectMapper.writeValueAsString(WorkspaceModuleStatus.ACTIVE)
        assertEquals("\"ACTIVE\"", result)
        
        val installingResult = objectMapper.writeValueAsString(WorkspaceModuleStatus.INSTALLING)
        assertEquals("\"INSTALLING\"", installingResult)
    }

    @Test
    fun `ModuleStatus in object should serialize correctly`() {
        val objectMapper = ObjectMapper()
        
        data class TestObject(val status: ModuleStatus)
        val testObj = TestObject(ModuleStatus.ACTIVE)
        
        val result = objectMapper.writeValueAsString(testObj)
        assertEquals("{\"status\":\"ACTIVE\"}", result)
    }
}