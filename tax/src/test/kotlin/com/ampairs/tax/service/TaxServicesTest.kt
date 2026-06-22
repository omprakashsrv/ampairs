package com.ampairs.tax.service

import com.ampairs.core.exception.NotFoundException
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.tax.domain.dto.ComponentCompositionDto
import com.ampairs.tax.domain.dto.ComponentReferenceDto
import com.ampairs.tax.domain.dto.SubscribeTaxCodeRequest
import com.ampairs.tax.domain.dto.UpdateTaxCodeRequest
import com.ampairs.tax.domain.dto.UpdateTaxConfigurationRequest
import com.ampairs.tax.domain.dto.UpdateTaxRuleRequest
import com.ampairs.tax.domain.model.ComponentComposition
import com.ampairs.tax.domain.model.ComponentReference
import com.ampairs.tax.domain.model.MasterTaxCode
import com.ampairs.tax.domain.model.MasterTaxComponent
import com.ampairs.tax.domain.model.MasterTaxRule
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.domain.model.TaxComponent
import com.ampairs.tax.domain.model.TaxConfiguration
import com.ampairs.tax.domain.model.TaxRule
import com.ampairs.tax.repository.MasterTaxCodeRepository
import com.ampairs.tax.repository.MasterTaxComponentRepository
import com.ampairs.tax.repository.MasterTaxRuleRepository
import com.ampairs.tax.repository.TaxCodeRepository
import com.ampairs.tax.repository.TaxComponentRepository
import com.ampairs.tax.repository.TaxConfigurationRepository
import com.ampairs.tax.repository.TaxRuleRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaxServicesTest {

    @Mock private lateinit var taxCodeRepository: TaxCodeRepository
    @Mock private lateinit var masterTaxCodeRepository: MasterTaxCodeRepository
    @Mock private lateinit var taxRuleRepository: TaxRuleRepository
    @Mock private lateinit var masterTaxRuleRepository: MasterTaxRuleRepository
    @Mock private lateinit var masterTaxComponentRepository: MasterTaxComponentRepository
    @Mock private lateinit var taxComponentRepository: TaxComponentRepository
    @Mock private lateinit var taxConfigurationRepository: TaxConfigurationRepository
    @Mock private lateinit var entityChangePublisher: EntityChangePublisher

    private lateinit var gstRuleTemplateService: GstRuleTemplateService
    private lateinit var masterTaxRuleService: MasterTaxRuleService
    private lateinit var masterTaxComponentService: MasterTaxComponentService
    private lateinit var masterTaxCodeService: MasterTaxCodeService
    private lateinit var taxCodeService: TaxCodeService
    private lateinit var taxRuleService: TaxRuleService
    private lateinit var taxComponentService: TaxComponentService
    private lateinit var taxConfigurationService: TaxConfigurationServiceV2

    @BeforeEach
    fun setUp() {
        gstRuleTemplateService = GstRuleTemplateService()
        masterTaxRuleService = MasterTaxRuleService(masterTaxRuleRepository)
        masterTaxComponentService = MasterTaxComponentService(masterTaxComponentRepository)
        masterTaxCodeService = MasterTaxCodeService(masterTaxCodeRepository)
        taxCodeService = TaxCodeService(
            taxCodeRepository,
            masterTaxCodeRepository,
            taxRuleRepository,
            masterTaxRuleService,
            masterTaxComponentService,
            taxComponentRepository,
            gstRuleTemplateService,
        )
        taxRuleService = TaxRuleService(taxRuleRepository, entityChangePublisher)
        taxComponentService = TaxComponentService(taxComponentRepository)
        taxConfigurationService = TaxConfigurationServiceV2(taxConfigurationRepository, entityChangePublisher)

        whenever(taxCodeRepository.save(any<TaxCode>())).thenAnswer { it.arguments[0] }
        whenever(taxRuleRepository.save(any<TaxRule>())).thenAnswer { it.arguments[0] }
        whenever(taxComponentRepository.save(any<TaxComponent>())).thenAnswer { it.arguments[0] }
        whenever(taxConfigurationRepository.save(any<TaxConfiguration>())).thenAnswer { it.arguments[0] }
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clearTenantContext()
    }

    // ---------- helpers ----------

    private fun masterCode(block: MasterTaxCode.() -> Unit = {}) = MasterTaxCode().apply {
        uid = "MTC-1"
        countryCode = "IN"
        codeType = "HSN_CODE"
        code = "1001"
        description = "Wheat"
        shortDescription = "Wheat"
        defaultTaxRate = 18.0
        isActive = true
        createdAt = Instant.parse("2025-01-01T00:00:00Z")
        updatedAt = Instant.parse("2025-01-02T00:00:00Z")
        block()
    }

    private fun workspaceCode(block: TaxCode.() -> Unit = {}) = TaxCode().apply {
        uid = "TC-1"
        masterTaxCodeId = "MTC-1"
        code = "1001"
        codeType = "HSN_CODE"
        description = "Wheat"
        shortDescription = "Wheat"
        isActive = true
        addedAt = Instant.parse("2025-01-01T00:00:00Z")
        updatedAt = Instant.parse("2025-01-02T00:00:00Z")
        block()
    }

    // ==================== TaxCodeService.subscribe ====================

    @Test
    fun `subscribe creates a new workspace tax code and auto-creates a GST rule`() {
        whenever(masterTaxCodeRepository.findByUid("MTC-1")).thenReturn(masterCode())
        whenever(taxCodeRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(null)
        whenever(masterTaxRuleRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(emptyList())
        whenever(taxComponentRepository.findByUid(any())).thenReturn(null)

        val result = taxCodeService.subscribe(
            SubscribeTaxCodeRequest(masterTaxCodeId = "MTC-1", isFavorite = true, notes = "n", customName = "Wheat tax")
        )

        assertEquals("MTC-1", result.masterTaxCodeId)
        assertEquals("1001", result.code)
        assertEquals("Wheat tax", result.customName)
        assertTrue(result.isFavorite)
        assertTrue(result.isActive)
        // Fallback composition path created CGST/SGST(9 each) + IGST(18) components
        verify(taxComponentRepository, org.mockito.kotlin.times(3)).save(any<TaxComponent>())
        verify(taxRuleRepository).save(any<TaxRule>())
    }

    @Test
    fun `subscribe is idempotent for an already-active subscription`() {
        val existing = workspaceCode { isFavorite = false; notes = null }
        whenever(masterTaxCodeRepository.findByUid("MTC-1")).thenReturn(masterCode())
        whenever(taxCodeRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(existing)

        val result = taxCodeService.subscribe(
            SubscribeTaxCodeRequest(masterTaxCodeId = "MTC-1", isFavorite = true, notes = "updated", customName = "X")
        )

        assertEquals("X", result.customName)
        assertEquals("updated", result.notes)
        assertTrue(result.isFavorite)
        // idempotent active branch does NOT create a new rule
        verify(taxRuleRepository, never()).save(any<TaxRule>())
    }

    @Test
    fun `subscribe reactivates a soft-deleted subscription and reactivates its rule`() {
        val existing = workspaceCode { isActive = false }
        val inactiveRule = TaxRule().apply { uid = "TR-1"; taxCodeId = "TC-1"; isActive = false }
        whenever(masterTaxCodeRepository.findByUid("MTC-1")).thenReturn(masterCode())
        whenever(taxCodeRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(existing)
        whenever(taxRuleRepository.findByTaxCodeId("TC-1")).thenReturn(listOf(inactiveRule))

        val result = taxCodeService.subscribe(SubscribeTaxCodeRequest(masterTaxCodeId = "MTC-1"))

        assertTrue(result.isActive)
        assertTrue(inactiveRule.isActive) // reactivated in place
        verify(taxRuleRepository).save(inactiveRule)
    }

    @Test
    fun `subscribe reactivates and creates a default rule when none exists`() {
        val existing = workspaceCode { isActive = false }
        whenever(masterTaxCodeRepository.findByUid("MTC-1")).thenReturn(masterCode())
        whenever(taxCodeRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(existing)
        whenever(taxRuleRepository.findByTaxCodeId("TC-1")).thenReturn(emptyList())
        whenever(masterTaxRuleRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(emptyList())
        whenever(taxComponentRepository.findByUid(any())).thenReturn(null)

        taxCodeService.subscribe(SubscribeTaxCodeRequest(masterTaxCodeId = "MTC-1"))

        verify(taxRuleRepository).save(any<TaxRule>())
    }

    @Test
    fun `subscribe builds the rule from a master template when one exists`() {
        val composition = mapOf(
            "INTRA_STATE" to mapOf(
                "scenario" to "INTRA_STATE",
                "totalRate" to 18.0,
                "components" to listOf(
                    mapOf("id" to "COMP_CGST_9", "name" to "CGST", "rate" to 9.0, "order" to 1),
                    mapOf("id" to "COMP_SGST_9", "name" to "SGST", "rate" to 9.0, "order" to 2),
                ),
            ),
        )
        val masterRule = MasterTaxRule().apply {
            uid = "MTR-1"
            countryCode = "IN"
            masterTaxCodeId = "MTC-1"
            taxCode = "1001"
            taxCodeType = "HSN_CODE"
            taxRate = 18.0
            jurisdiction = "INDIA"
            jurisdictionLevel = "COUNTRY"
            componentComposition = composition
        }
        whenever(masterTaxCodeRepository.findByUid("MTC-1")).thenReturn(masterCode())
        whenever(taxCodeRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(null)
        whenever(masterTaxRuleRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(listOf(masterRule))
        whenever(taxComponentRepository.findByUid(any())).thenReturn(null)
        val masterComp = MasterTaxComponent().apply {
            uid = "COMP_CGST_9"; componentTypeId = "TYPE_CGST"; componentName = "CGST"
            componentDisplayName = "CGST 9%"; taxType = "GST"; jurisdiction = "INDIA"
            jurisdictionLevel = "COUNTRY"; ratePercentage = 9.0
        }
        whenever(masterTaxComponentRepository.findByUid(any())).thenReturn(masterComp)

        taxCodeService.subscribe(SubscribeTaxCodeRequest(masterTaxCodeId = "MTC-1"))

        // Two distinct component IDs from the template → two component creations
        verify(taxComponentRepository, org.mockito.kotlin.times(2)).save(any<TaxComponent>())
        verify(taxRuleRepository).save(any<TaxRule>())
    }

    @Test
    fun `subscribe does not auto-create a rule when no default tax rate`() {
        whenever(masterTaxCodeRepository.findByUid("MTC-1")).thenReturn(masterCode { defaultTaxRate = null })
        whenever(taxCodeRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(null)

        taxCodeService.subscribe(SubscribeTaxCodeRequest(masterTaxCodeId = "MTC-1"))

        verify(taxRuleRepository, never()).save(any<TaxRule>())
    }

    @Test
    fun `subscribe does not auto-create a rule for a non-IN country`() {
        whenever(masterTaxCodeRepository.findByUid("MTC-1")).thenReturn(masterCode { countryCode = "US" })
        whenever(taxCodeRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(null)

        taxCodeService.subscribe(SubscribeTaxCodeRequest(masterTaxCodeId = "MTC-1"))

        verify(taxRuleRepository, never()).save(any<TaxRule>())
    }

    @Test
    fun `subscribe with a custom rule id skips auto rule creation`() {
        whenever(masterTaxCodeRepository.findByUid("MTC-1")).thenReturn(masterCode())
        whenever(taxCodeRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(null)

        taxCodeService.subscribe(SubscribeTaxCodeRequest(masterTaxCodeId = "MTC-1", customTaxRuleId = "CUSTOM-1"))

        verify(taxRuleRepository, never()).save(any<TaxRule>())
    }

    @Test
    fun `subscribe throws NotFound when the master code is missing`() {
        whenever(masterTaxCodeRepository.findByUid("MTC-X")).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            taxCodeService.subscribe(SubscribeTaxCodeRequest(masterTaxCodeId = "MTC-X"))
        }
    }

    // ==================== TaxCodeService getTaxCodes / getById / unsubscribe / update ====================

    @Test
    fun `getTaxCodes returns all active when no cursor`() {
        whenever(taxCodeRepository.findAllActive(any())).thenReturn(PageImpl(listOf(workspaceCode())))

        val page = taxCodeService.getTaxCodes(modifiedAfter = null, page = 0, size = 50)

        assertEquals(1, page.content.size)
        assertEquals("TC-1", page.content.first().id)
        verify(taxCodeRepository).findAllActive(any())
        verify(taxCodeRepository, never()).findByUpdatedAtAfter(any(), any())
    }

    @Test
    fun `getTaxCodes uses the modifiedAfter cursor when provided`() {
        whenever(taxCodeRepository.findByUpdatedAtAfter(any(), any())).thenReturn(PageImpl(emptyList()))

        taxCodeService.getTaxCodes(modifiedAfter = 1_700_000_000_000L, page = 1, size = 10)

        verify(taxCodeRepository).findByUpdatedAtAfter(eq(Instant.ofEpochMilli(1_700_000_000_000L)), any())
    }

    @Test
    fun `getById returns the dto`() {
        whenever(taxCodeRepository.findByUid("TC-1")).thenReturn(workspaceCode())
        assertEquals("TC-1", taxCodeService.getById("TC-1").id)
    }

    @Test
    fun `getById throws NotFound`() {
        whenever(taxCodeRepository.findByUid("TC-X")).thenReturn(null)
        assertThrows(NotFoundException::class.java) { taxCodeService.getById("TC-X") }
    }

    @Test
    fun `unsubscribe soft-deletes the tax code`() {
        val code = workspaceCode()
        whenever(taxCodeRepository.findByUid("TC-1")).thenReturn(code)

        taxCodeService.unsubscribe("TC-1")

        assertFalse(code.isActive)
        verify(taxCodeRepository).save(code)
    }

    @Test
    fun `unsubscribe throws NotFound`() {
        whenever(taxCodeRepository.findByUid("TC-X")).thenReturn(null)
        assertThrows(NotFoundException::class.java) { taxCodeService.unsubscribe("TC-X") }
    }

    @Test
    fun `updateConfiguration applies only the supplied fields`() {
        val code = workspaceCode { isFavorite = false; notes = "old"; customName = "old" }
        whenever(taxCodeRepository.findByUid("TC-1")).thenReturn(code)

        val result = taxCodeService.updateConfiguration("TC-1", UpdateTaxCodeRequest(isFavorite = true, notes = "new"))

        assertTrue(result.isFavorite)
        assertEquals("new", result.notes)
        assertEquals("old", result.customName) // untouched
    }

    @Test
    fun `updateConfiguration throws NotFound`() {
        whenever(taxCodeRepository.findByUid("TC-X")).thenReturn(null)
        assertThrows(NotFoundException::class.java) {
            taxCodeService.updateConfiguration("TC-X", UpdateTaxCodeRequest(isFavorite = true))
        }
    }

    // ==================== TaxRuleService ====================

    private fun rule(block: TaxRule.() -> Unit = {}) = TaxRule().apply {
        uid = "TR-1"
        countryCode = "IN"
        taxCodeId = "TC-1"
        taxCode = "1001"
        taxCodeType = "HSN_CODE"
        jurisdiction = "INDIA"
        jurisdictionLevel = "COUNTRY"
        componentComposition = mapOf(
            "INTRA_STATE" to ComponentComposition(
                scenario = "INTRA_STATE",
                components = listOf(ComponentReference("COMP_CGST_9", "CGST", 9.0, 1)),
                totalRate = 18.0,
            )
        )
        isActive = true
        block()
    }

    @Test
    fun `getTaxRules returns all when no filters`() {
        whenever(taxRuleRepository.findAll(any<Pageable>())).thenReturn(PageImpl(listOf(rule())))
        val page = taxRuleService.getTaxRules(modifiedAfter = null, taxCode = null, page = 0, size = 20)
        assertEquals(1, page.content.size)
        assertEquals(18.0, page.content.first().componentComposition["INTRA_STATE"]!!.totalRate)
    }

    @Test
    fun `getTaxRules filters by taxCode`() {
        whenever(taxRuleRepository.findByTaxCode(eq("1001"), any())).thenReturn(PageImpl(listOf(rule())))
        taxRuleService.getTaxRules(modifiedAfter = null, taxCode = "1001", page = 0, size = 20)
        verify(taxRuleRepository).findByTaxCode(eq("1001"), any())
    }

    @Test
    fun `getTaxRules filters by modifiedAfter cursor`() {
        whenever(taxRuleRepository.findByUpdatedAtAfter(any(), any())).thenReturn(PageImpl(emptyList()))
        taxRuleService.getTaxRules(modifiedAfter = 1000L, taxCode = "1001", page = 0, size = 20)
        verify(taxRuleRepository).findByUpdatedAtAfter(eq(Instant.ofEpochMilli(1000L)), any())
    }

    @Test
    fun `findByTaxCodeId maps rules`() {
        whenever(taxRuleRepository.findByTaxCodeId("TC-1")).thenReturn(listOf(rule()))
        val dtos = taxRuleService.findByTaxCodeId("TC-1")
        assertEquals(1, dtos.size)
        assertEquals("TR-1", dtos.first().id)
    }

    @Test
    fun `tax rule getById returns and throws`() {
        whenever(taxRuleRepository.findByUid("TR-1")).thenReturn(rule())
        assertEquals("TR-1", taxRuleService.getById("TR-1").id)
        whenever(taxRuleRepository.findByUid("TR-X")).thenReturn(null)
        assertThrows(NotFoundException::class.java) { taxRuleService.getById("TR-X") }
    }

    @Test
    fun `updateTaxRule applies fields and publishes change`() {
        val r = rule()
        whenever(taxRuleRepository.findByUid("TR-1")).thenReturn(r)

        val result = taxRuleService.updateTaxRule(
            "TR-1",
            UpdateTaxRuleRequest(
                jurisdiction = "MAHARASHTRA",
                jurisdictionLevel = "STATE",
                isActive = false,
                componentComposition = mapOf(
                    "INTER_STATE" to ComponentCompositionDto(
                        scenario = "INTER_STATE",
                        components = listOf(ComponentReferenceDto("COMP_IGST_18", "IGST", 18.0, 1)),
                        totalRate = 18.0,
                    )
                ),
            ),
        )

        assertEquals("MAHARASHTRA", result.jurisdiction)
        assertEquals("STATE", result.jurisdictionLevel)
        assertFalse(result.isActive)
        assertEquals(18.0, result.componentComposition["INTER_STATE"]!!.totalRate)
        verify(entityChangePublisher).updated("tax", "TR-1")
    }

    @Test
    fun `updateTaxRule throws NotFound`() {
        whenever(taxRuleRepository.findByUid("TR-X")).thenReturn(null)
        assertThrows(NotFoundException::class.java) {
            taxRuleService.updateTaxRule("TR-X", UpdateTaxRuleRequest(jurisdiction = "X"))
        }
    }

    @Test
    fun `deleteTaxRule soft-deletes and publishes`() {
        val r = rule()
        whenever(taxRuleRepository.findByUid("TR-1")).thenReturn(r)
        taxRuleService.deleteTaxRule("TR-1")
        assertFalse(r.isActive)
        verify(taxRuleRepository).save(r)
        verify(entityChangePublisher).updated("tax", "TR-1")
    }

    @Test
    fun `deleteTaxRule throws NotFound`() {
        whenever(taxRuleRepository.findByUid("TR-X")).thenReturn(null)
        assertThrows(NotFoundException::class.java) { taxRuleService.deleteTaxRule("TR-X") }
    }

    // ==================== TaxComponentService ====================

    private fun component(block: TaxComponent.() -> Unit = {}) = TaxComponent().apply {
        uid = "TCMP-1"
        componentTypeId = "TYPE_CGST"
        componentName = "CGST"
        componentDisplayName = "CGST 9%"
        taxType = "GST"
        jurisdiction = "INDIA"
        jurisdictionLevel = "COUNTRY"
        ratePercentage = 9.0
        isCompound = false
        calculationMethod = "PERCENTAGE"
        isActive = true
        createdAt = Instant.parse("2025-01-01T00:00:00Z")
        updatedAt = Instant.parse("2025-01-02T00:00:00Z")
        block()
    }

    @Test
    fun `getTaxComponents - no filter returns active`() {
        whenever(taxComponentRepository.findAllActive(any())).thenReturn(PageImpl(listOf(component())))
        val page = taxComponentService.getTaxComponents(null, null, null, 0, 20)
        assertEquals(9.0, page.content.first().ratePercentage)
    }

    @Test
    fun `getTaxComponents - filters by modifiedAfter, taxType, jurisdiction`() {
        whenever(taxComponentRepository.findByUpdatedAtAfter(any(), any())).thenReturn(PageImpl(emptyList()))
        taxComponentService.getTaxComponents(50L, null, null, 0, 20)
        verify(taxComponentRepository).findByUpdatedAtAfter(eq(Instant.ofEpochMilli(50L)), any())

        whenever(taxComponentRepository.findByTaxType(eq("GST"), any())).thenReturn(PageImpl(emptyList()))
        taxComponentService.getTaxComponents(null, "GST", null, 0, 20)
        verify(taxComponentRepository).findByTaxType(eq("GST"), any())

        whenever(taxComponentRepository.findByJurisdiction(eq("INDIA"), any())).thenReturn(PageImpl(emptyList()))
        taxComponentService.getTaxComponents(null, null, "INDIA", 0, 20)
        verify(taxComponentRepository).findByJurisdiction(eq("INDIA"), any())
    }

    @Test
    fun `getWorkspaceComponents normalises COUNTRY level to FEDERAL`() {
        whenever(taxComponentRepository.findAllActive(any())).thenReturn(PageImpl(listOf(component())))
        val page = taxComponentService.getWorkspaceComponents(null, 0, 20)
        assertEquals("FEDERAL", page.content.first().jurisdictionLevel)
        assertEquals(9.0, page.content.first().ratePercentage)
    }

    @Test
    fun `getWorkspaceComponents keeps STATE level and uses cursor`() {
        whenever(taxComponentRepository.findByUpdatedAtAfter(any(), any()))
            .thenReturn(PageImpl(listOf(component { jurisdictionLevel = "STATE" })))
        val page = taxComponentService.getWorkspaceComponents(99L, 0, 20)
        assertEquals("STATE", page.content.first().jurisdictionLevel)
        verify(taxComponentRepository).findByUpdatedAtAfter(eq(Instant.ofEpochMilli(99L)), any())
    }

    // ==================== TaxConfigurationServiceV2 ====================

    private fun configuration(block: TaxConfiguration.() -> Unit = {}) = TaxConfiguration().apply {
        uid = "TCFG-1"
        countryCode = "IN"
        gstin = "27AAAAA0000A1Z5"
        taxStrategy = "INDIA_GST"
        defaultTaxCodeSystem = "HSN_CODE"
        taxJurisdictions = listOf("MAHARASHTRA")
        autoSubscribeNewCodes = true
        block()
    }

    @Test
    fun `getConfiguration returns the workspace config`() {
        TenantContextHolder.setCurrentTenant("WS-1")
        whenever(taxConfigurationRepository.findByOwnerId("WS-1")).thenReturn(configuration())

        val dto = taxConfigurationService.getConfiguration()

        assertEquals("INDIA_GST", dto.taxStrategy)
        assertEquals("27AAAAA0000A1Z5", dto.gstin)
    }

    @Test
    fun `getConfiguration throws NotFound when absent`() {
        TenantContextHolder.setCurrentTenant("WS-1")
        whenever(taxConfigurationRepository.findByOwnerId("WS-1")).thenReturn(null)
        assertThrows(NotFoundException::class.java) { taxConfigurationService.getConfiguration() }
    }

    @Test
    fun `createConfiguration persists and publishes`() {
        TenantContextHolder.setCurrentTenant("WS-1")
        whenever(taxConfigurationRepository.findByOwnerId("WS-1")).thenReturn(null)

        val dto = taxConfigurationService.createConfiguration(
            UpdateTaxConfigurationRequest(
                countryCode = "IN",
                taxStrategy = "INDIA_GST",
                defaultTaxCodeSystem = "HSN_CODE",
                gstin = "27AAAAA0000A1Z5",
                taxJurisdictions = listOf("MAHARASHTRA"),
                autoSubscribeNewCodes = true,
            )
        )

        assertEquals("IN", dto.countryCode)
        assertEquals("INDIA_GST", dto.taxStrategy)
        assertTrue(dto.autoSubscribeNewCodes)
        verify(entityChangePublisher).updated(eq("tax"), any())
    }

    @Test
    fun `createConfiguration rejects when already existing`() {
        TenantContextHolder.setCurrentTenant("WS-1")
        whenever(taxConfigurationRepository.findByOwnerId("WS-1")).thenReturn(configuration())
        assertThrows(IllegalStateException::class.java) {
            taxConfigurationService.createConfiguration(
                UpdateTaxConfigurationRequest(countryCode = "IN", taxStrategy = "INDIA_GST", defaultTaxCodeSystem = "HSN_CODE")
            )
        }
    }

    @Test
    fun `createConfiguration requires mandatory fields`() {
        TenantContextHolder.setCurrentTenant("WS-1")
        whenever(taxConfigurationRepository.findByOwnerId("WS-1")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) {
            taxConfigurationService.createConfiguration(UpdateTaxConfigurationRequest(taxStrategy = "INDIA_GST", defaultTaxCodeSystem = "HSN_CODE"))
        }
    }

    @Test
    fun `updateConfiguration applies supplied fields only`() {
        TenantContextHolder.setCurrentTenant("WS-1")
        val cfg = configuration()
        whenever(taxConfigurationRepository.findByOwnerId("WS-1")).thenReturn(cfg)

        val dto = taxConfigurationService.updateConfiguration(
            UpdateTaxConfigurationRequest(gstin = "29BBBBB1111B2Z6", autoSubscribeNewCodes = false)
        )

        assertEquals("29BBBBB1111B2Z6", dto.gstin)
        assertFalse(dto.autoSubscribeNewCodes)
        assertEquals("INDIA_GST", dto.taxStrategy) // untouched
        verify(entityChangePublisher).updated("tax", "TCFG-1")
    }

    @Test
    fun `updateConfiguration throws NotFound when config absent`() {
        TenantContextHolder.setCurrentTenant("WS-1")
        whenever(taxConfigurationRepository.findByOwnerId("WS-1")).thenReturn(null)
        assertThrows(NotFoundException::class.java) {
            taxConfigurationService.updateConfiguration(UpdateTaxConfigurationRequest(gstin = "X"))
        }
    }

    // ==================== MasterTaxCodeService ====================

    @Test
    fun `master code search and getPopular map dtos`() {
        whenever(masterTaxCodeRepository.searchCodes(any(), any(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(PageImpl(listOf(masterCode())))
        assertEquals("MTC-1", masterTaxCodeService.search("wheat", "IN", "HSN_CODE", null, 0, 20).content.first().id)

        whenever(masterTaxCodeRepository.findPopularCodes(any(), anyOrNull(), any()))
            .thenReturn(PageImpl(listOf(masterCode())))
        assertEquals(18.0, masterTaxCodeService.getPopular("IN", null, 0, 20).content.first().defaultTaxRate)
    }

    @Test
    fun `master code getById returns and throws`() {
        whenever(masterTaxCodeRepository.findByUid("MTC-1")).thenReturn(masterCode())
        assertEquals("1001", masterTaxCodeService.getById("MTC-1").code)
        whenever(masterTaxCodeRepository.findByUid("MTC-X")).thenReturn(null)
        assertThrows(NotFoundException::class.java) { masterTaxCodeService.getById("MTC-X") }
    }

    // ==================== MasterTaxComponentService ====================

    private fun masterComponent(block: MasterTaxComponent.() -> Unit = {}) = MasterTaxComponent().apply {
        uid = "MCMP-1"
        componentTypeId = "TYPE_CGST"
        componentName = "CGST"
        componentDisplayName = "CGST 9%"
        taxType = "GST"
        jurisdiction = "INDIA"
        jurisdictionLevel = "COUNTRY"
        ratePercentage = 9.0
        isActive = true
        block()
    }

    @Test
    fun `master component getAll and search map dtos`() {
        whenever(masterTaxComponentRepository.findByIsActiveTrue(any())).thenReturn(PageImpl(listOf(masterComponent())))
        assertEquals(9.0, masterTaxComponentService.getAllComponents(0, 20).content.first().ratePercentage)

        whenever(masterTaxComponentRepository.searchComponents(anyOrNull(), anyOrNull(), any())).thenReturn(PageImpl(listOf(masterComponent())))
        assertEquals("MCMP-1", masterTaxComponentService.searchComponents("TYPE_CGST", "INDIA", 0, 20).content.first().id)
    }

    @Test
    fun `master component getById returns and throws`() {
        whenever(masterTaxComponentRepository.findByUid("MCMP-1")).thenReturn(masterComponent())
        assertEquals("CGST", masterTaxComponentService.getComponentById("MCMP-1").componentName)
        whenever(masterTaxComponentRepository.findByUid("MCMP-X")).thenReturn(null)
        assertThrows(NotFoundException::class.java) { masterTaxComponentService.getComponentById("MCMP-X") }
    }

    @Test
    fun `findComponentsByType maps content`() {
        whenever(masterTaxComponentRepository.searchComponents(eq("TYPE_CGST"), eq(null), any()))
            .thenReturn(PageImpl(listOf(masterComponent())))
        val list = masterTaxComponentService.findComponentsByType("TYPE_CGST")
        assertEquals(1, list.size)
    }

    @Test
    fun `getComponentTypesForCountry returns the GST set for IN and empty for unknown`() {
        assertEquals(5, masterTaxComponentService.getComponentTypesForCountry("in").size)
        assertEquals(1, masterTaxComponentService.getComponentTypesForCountry("US").size)
        assertEquals(1, masterTaxComponentService.getComponentTypesForCountry("GB").size)
        assertTrue(masterTaxComponentService.getComponentTypesForCountry("FR").isEmpty())
    }

    // ==================== MasterTaxRuleService ====================

    private fun masterRule(block: MasterTaxRule.() -> Unit = {}) = MasterTaxRule().apply {
        uid = "MTR-1"
        countryCode = "IN"
        masterTaxCodeId = "MTC-1"
        taxCode = "1001"
        taxCodeType = "HSN_CODE"
        taxRate = 18.0
        jurisdiction = "INDIA"
        jurisdictionLevel = "COUNTRY"
        componentComposition = emptyMap()
        isActive = true
        block()
    }

    @Test
    fun `master rule getAll and search map dtos`() {
        whenever(masterTaxRuleRepository.findByIsActiveTrue(any())).thenReturn(PageImpl(listOf(masterRule())))
        assertEquals(18.0, masterTaxRuleService.getAllRules(0, 20).content.first().taxRate)

        whenever(masterTaxRuleRepository.searchRules(eq("IN"), any(), any())).thenReturn(PageImpl(listOf(masterRule())))
        assertEquals("MTR-1", masterTaxRuleService.searchRules("in", "HSN_CODE", 0, 20).content.first().id)
    }

    @Test
    fun `master rule getById returns and throws`() {
        whenever(masterTaxRuleRepository.findByUid("MTR-1")).thenReturn(masterRule())
        assertEquals("1001", masterTaxRuleService.getRuleById("MTR-1").taxCode)
        whenever(masterTaxRuleRepository.findByUid("MTR-X")).thenReturn(null)
        assertThrows(NotFoundException::class.java) { masterTaxRuleService.getRuleById("MTR-X") }
    }

    @Test
    fun `findRulesByMasterTaxCode and getRuleByMasterTaxCodeId`() {
        whenever(masterTaxRuleRepository.findByMasterTaxCodeId("MTC-1")).thenReturn(listOf(masterRule()))
        assertEquals(1, masterTaxRuleService.findRulesByMasterTaxCode("MTC-1").size)
        assertNotNull(masterTaxRuleService.getRuleByMasterTaxCodeId("MTC-1"))

        whenever(masterTaxRuleRepository.findByMasterTaxCodeId("MTC-EMPTY")).thenReturn(emptyList())
        assertNull(masterTaxRuleService.getRuleByMasterTaxCodeId("MTC-EMPTY"))
    }

    // ==================== GstRuleTemplateService ====================

    @Test
    fun `standard GST composition splits CGST and SGST and uses single IGST`() {
        val comp = gstRuleTemplateService.generateStandardGstComposition(18.0)
        val intra = comp["INTRA_STATE"]!!
        assertEquals(2, intra.components.size)
        assertEquals(9.0, intra.components[0].rate)
        assertEquals(9.0, intra.components[1].rate)
        assertEquals(18.0, intra.totalRate)
        val inter = comp["INTER_STATE"]!!
        assertEquals(1, inter.components.size)
        assertEquals(18.0, inter.components[0].rate)
        assertEquals("COMP_IGST_18", inter.components[0].id)
    }

    @Test
    fun `zero-rate GST composition produces zero components`() {
        val comp = gstRuleTemplateService.generateStandardGstComposition(0.0)
        assertEquals(0.0, comp["INTRA_STATE"]!!.totalRate)
        assertEquals(0.0, comp["INTER_STATE"]!!.components[0].rate)
    }

    @Test
    fun `GST with cess includes cess in both scenarios`() {
        val comp = gstRuleTemplateService.generateGstWithCess(baseRate = 28.0, cessRate = 12.0)
        val intra = comp["INTRA_STATE"]!!
        assertEquals(3, intra.components.size)
        assertEquals(40.0, intra.totalRate) // 28 base + 12 cess
        assertEquals("CESS", intra.components[2].name)
        val inter = comp["INTER_STATE"]!!
        assertEquals(2, inter.components.size)
        assertEquals(28.0, inter.components[0].rate)
        assertEquals(12.0, inter.components[1].rate)
    }

    @Test
    fun `isStandardGstRate recognises standard slabs`() {
        assertTrue(gstRuleTemplateService.isStandardGstRate(18.0))
        assertTrue(gstRuleTemplateService.isStandardGstRate(0.0))
        assertFalse(gstRuleTemplateService.isStandardGstRate(7.5))
    }
}
