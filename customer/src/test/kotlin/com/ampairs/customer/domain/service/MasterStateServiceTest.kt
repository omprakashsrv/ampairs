package com.ampairs.customer.domain.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.customer.domain.model.MasterState
import com.ampairs.customer.domain.model.State
import com.ampairs.customer.repository.MasterStateRepository
import com.ampairs.customer.repository.StateRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MasterStateServiceTest {

    private val masterStateRepository: MasterStateRepository = mock()
    private val stateRepository: StateRepository = mock()

    private lateinit var service: MasterStateService

    private fun masterState(code: String, block: MasterState.() -> Unit = {}): MasterState =
        MasterState().apply {
            uid = "MST-$code"
            stateCode = code
            name = "Name-$code"
            shortName = code
            countryCode = "IN"
            countryName = "India"
            active = true
            block()
        }

    @BeforeEach
    fun setUp() {
        service = MasterStateService(masterStateRepository, stateRepository)
        whenever(stateRepository.save(any<State>())).thenAnswer { it.arguments[0] }
    }

    @AfterEach
    fun tearDown() = TenantContextHolder.clearTenantContext()

    @Test
    fun `importStateToWorkspace returns null when master state not found`() {
        whenever(masterStateRepository.findByStateCode("ZZ")).thenReturn(null)

        assertNull(service.importStateToWorkspace("ZZ"))
        verify(stateRepository, never()).save(any<State>())
    }

    @Test
    fun `importStateToWorkspace returns existing state without re-saving`() {
        val ms = masterState("KA")
        val existing = State().apply { uid = "STA-KA"; masterStateCode = "KA" }
        whenever(masterStateRepository.findByStateCode("KA")).thenReturn(ms)
        whenever(stateRepository.findFirstByMasterStateCode("KA")).thenReturn(existing)

        val result = service.importStateToWorkspace("KA")

        assertSame(existing, result)
        verify(stateRepository, never()).save(any<State>())
    }

    @Test
    fun `importStateToWorkspace imports a new workspace state from master`() {
        val ms = masterState("MH") { name = "Maharashtra"; shortName = "MH"; countryName = "India" }
        whenever(masterStateRepository.findByStateCode("MH")).thenReturn(ms)
        whenever(stateRepository.findFirstByMasterStateCode("MH")).thenReturn(null)

        val result = service.importStateToWorkspace("MH")

        assertEquals("Maharashtra", result?.name)
        assertEquals("MH", result?.masterStateCode)
        assertEquals("India", result?.country)
        assertSame(ms, result?.masterState)
        verify(stateRepository).save(any<State>())
    }

    @Test
    fun `importStatesToWorkspace skips unknown codes and collects imported ones`() {
        val ms = masterState("GA")
        whenever(masterStateRepository.findByStateCode("GA")).thenReturn(ms)
        whenever(stateRepository.findFirstByMasterStateCode("GA")).thenReturn(null)
        whenever(masterStateRepository.findByStateCode("ZZ")).thenReturn(null)

        val result = service.importStatesToWorkspace(listOf("GA", "ZZ"))

        assertEquals(1, result.size)
        assertEquals("GA", result.single().masterStateCode)
    }

    @Test
    fun `getAvailableStatesForImport throws when no tenant context`() {
        TenantContextHolder.clearTenantContext()

        assertThrows(IllegalStateException::class.java) {
            service.getAvailableStatesForImport()
        }
    }

    @Test
    fun `getAvailableStatesForImport filters out already-imported states`() {
        TenantContextHolder.setCurrentTenant("ws-1")
        val importedState = State().apply { uid = "STA-KA"; masterStateCode = "KA" }
        whenever(stateRepository.findByOwnerId("ws-1")).thenReturn(listOf(importedState))
        whenever(masterStateRepository.findByActiveTrueOrderByNameAsc())
            .thenReturn(listOf(masterState("KA"), masterState("MH"), masterState("GA")))

        val result = service.getAvailableStatesForImport()

        val codes = result.map { it.stateCode }.toSet()
        assertEquals(setOf("MH", "GA"), codes)
    }
}
