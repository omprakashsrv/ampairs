package com.ampairs.tests.integration

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

/**
 * Integration tests for complete product catalog management workflow.
 * 
 * Tests the end-to-end process of creating, managing, and searching products
 * across different retail business types with proper inventory management.
 * 
 * ⚠️ CRITICAL: These tests MUST FAIL initially (no implementation yet)
 * Following TDD principles: RED → GREEN → REFACTOR
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ProductCatalogWorkflowIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private var hardwareWorkspaceId: String = ""
    private var jewelryWorkspaceId: String = ""
    private var authToken: String = "valid_jwt_token"

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @Order(1)
    @DisplayName("Setup: Create HARDWARE and JEWELRY workspaces for product catalog testing")
    fun `should create test workspaces for different business types`() {
        // Create Hardware workspace
        val hardwareWorkspaceRequest = """
            {
                "name": "BuildMart Hardware Store",
                "description": "Complete construction materials and tools supplier",
                "business_type": "HARDWARE",
                "owner_details": {
                    "name": "Suresh Reddy",
                    "phone": "+919876543220",
                    "email": "suresh@buildmart.com"
                },
                "business_details": {
                    "gstin": "36FGHIJ5678K1L2",
                    "pan": "FGHIJ5678K"
                }
            }
        """.trimIndent()

        val hardwareResponse = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .body(hardwareWorkspaceRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.business_type", equalTo("HARDWARE"))
            .extract()

        hardwareWorkspaceId = hardwareResponse.path("data.id")

        // Create Jewelry workspace
        val jewelryWorkspaceRequest = """
            {
                "name": "Golden Dreams Jewelers",
                "description": "Premium gold and diamond jewelry boutique",
                "business_type": "JEWELRY",
                "owner_details": {
                    "name": "Lakshmi Devi",
                    "phone": "+919876543221",
                    "email": "lakshmi@goldendreams.com"
                },
                "business_details": {
                    "gstin": "29KLMNO9012P3Q4",
                    "pan": "KLMNO9012P"
                }
            }
        """.trimIndent()

        val jewelryResponse = RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .body(jewelryWorkspaceRequest)
            .`when`()
            .post("/workspace/v1")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.business_type", equalTo("JEWELRY"))
            .extract()

        jewelryWorkspaceId = jewelryResponse.path("data.id")
    }

    @Test
    @Order(2)
    @DisplayName("Step 1: Create hardware product categories with construction focus")
    fun `should create hardware product categories with proper attributes`() {
        val hardwareCategories = listOf(
            """
            {
                "name": "Cement & Concrete",
                "code": "CEMENT",
                "description": "Cement, concrete mix, additives",
                "business_type": "HARDWARE",
                "attributes": ["brand", "grade", "bag_weight", "setting_time"],
                "measurement_units": ["bags", "tons"]
            }
            """,
            """
            {
                "name": "Steel & Iron",
                "code": "STEEL",
                "description": "TMT bars, angles, pipes, sheets",
                "business_type": "HARDWARE",
                "attributes": ["grade", "thickness", "length", "diameter", "brand"],
                "measurement_units": ["pieces", "tons", "meters"]
            }
            """,
            """
            {
                "name": "Hand Tools",
                "code": "TOOLS",
                "description": "Hammers, screwdrivers, pliers, wrenches",
                "business_type": "HARDWARE",
                "attributes": ["brand", "material", "size", "warranty_months"],
                "measurement_units": ["pieces"]
            }
            """,
            """
            {
                "name": "Electrical Items",
                "code": "ELECTRICAL",
                "description": "Wires, switches, MCBs, LED bulbs",
                "business_type": "HARDWARE",
                "attributes": ["brand", "rating", "color", "warranty_months"],
                "measurement_units": ["pieces", "meters", "rolls"]
            }
            """
        )

        hardwareCategories.forEach { categoryJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", hardwareWorkspaceId)
                .body(categoryJson.trimIndent())
                .`when`()
                .post("/category/v1")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.business_type", equalTo("HARDWARE"))
        }
    }

    @Test
    @Order(3)
    @DisplayName("Step 2: Create jewelry product categories with precious metals focus")
    fun `should create jewelry product categories with metal and gem attributes`() {
        val jewelryCategories = listOf(
            """
            {
                "name": "Gold Jewelry",
                "code": "GOLD",
                "description": "Gold rings, necklaces, earrings, bangles",
                "business_type": "JEWELRY",
                "attributes": ["purity", "weight_grams", "making_charges", "stone_type", "design_type"],
                "measurement_units": ["pieces", "grams"]
            }
            """,
            """
            {
                "name": "Silver Jewelry",
                "code": "SILVER",
                "description": "Silver ornaments, utensils, gifts",
                "business_type": "JEWELRY",
                "attributes": ["purity", "weight_grams", "finish_type", "design_type"],
                "measurement_units": ["pieces", "grams"]
            }
            """,
            """
            {
                "name": "Diamond Jewelry",
                "code": "DIAMOND",
                "description": "Diamond rings, pendants, earrings",
                "business_type": "JEWELRY",
                "attributes": ["gold_purity", "diamond_carats", "diamond_clarity", "diamond_color", "certification"],
                "measurement_units": ["pieces"]
            }
            """,
            """
            {
                "name": "Precious Stones",
                "code": "STONES",
                "description": "Emerald, ruby, sapphire, pearl",
                "business_type": "JEWELRY",
                "attributes": ["stone_type", "weight_carats", "origin", "treatment", "certification"],
                "measurement_units": ["pieces", "carats"]
            }
            """
        )

        jewelryCategories.forEach { categoryJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", jewelryWorkspaceId)
                .body(categoryJson.trimIndent())
                .`when`()
                .post("/category/v1")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.business_type", equalTo("JEWELRY"))
        }
    }

    @Test
    @Order(4)
    @DisplayName("Step 3: Create comprehensive hardware product catalog")
    fun `should create diverse hardware products with proper specifications`() {
        val hardwareProducts = listOf(
            // Cement products
            """
            {
                "name": "ACC Gold Water Resistant Cement",
                "sku": "CEMENT-ACC-GOLD-50KG",
                "description": "Premium quality OPC cement with water resistance",
                "category_code": "CEMENT",
                "unit_id": "unit-bags",
                "tax_code_id": "tax-gst-28",
                "base_price": 340.00,
                "cost_price": 315.00,
                "attributes": {
                    "brand": "ACC",
                    "grade": "53 Grade",
                    "bag_weight": "50 KG",
                    "setting_time": "30 minutes",
                    "compressive_strength": "53 MPa"
                }
            }
            """,
            """
            {
                "name": "Ultratech Ready Mix Concrete",
                "sku": "CONCRETE-ULTRA-M25",
                "description": "Ready mix concrete M25 grade for construction",
                "category_code": "CEMENT",
                "unit_id": "unit-cubic-meters",
                "tax_code_id": "tax-gst-28",
                "base_price": 4200.00,
                "cost_price": 3800.00,
                "attributes": {
                    "brand": "Ultratech",
                    "grade": "M25",
                    "setting_time": "45 minutes",
                    "slump": "75-100mm"
                }
            }
            """,
            // Steel products  
            """
            {
                "name": "TATA TMT Steel Bars 12mm",
                "sku": "STEEL-TATA-TMT-12MM",
                "description": "High strength TMT bars for construction",
                "category_code": "STEEL",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-18",
                "base_price": 680.00,
                "cost_price": 650.00,
                "attributes": {
                    "brand": "TATA",
                    "grade": "Fe 500D",
                    "diameter": "12mm",
                    "length": "12 meters",
                    "tensile_strength": "500 N/mm²"
                }
            }
            """,
            """
            {
                "name": "MS Angle Iron 40x40x5mm",
                "sku": "STEEL-ANGLE-40X40X5",
                "description": "Mild steel angle for structural work",
                "category_code": "STEEL", 
                "unit_id": "unit-meters",
                "tax_code_id": "tax-gst-18",
                "base_price": 85.00,
                "cost_price": 75.00,
                "attributes": {
                    "grade": "IS 2062",
                    "thickness": "5mm",
                    "dimensions": "40x40mm",
                    "length": "6 meters"
                }
            }
            """,
            // Tools
            """
            {
                "name": "Stanley Claw Hammer 450g",
                "sku": "TOOL-STANLEY-HAMMER-450G",
                "description": "Professional claw hammer with steel handle",
                "category_code": "TOOLS",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-18",
                "base_price": 850.00,
                "cost_price": 720.00,
                "attributes": {
                    "brand": "Stanley",
                    "material": "Steel",
                    "weight": "450g",
                    "warranty_months": "24"
                }
            }
            """,
            // Electrical
            """
            {
                "name": "Havells 2.5 sqmm Copper Wire",
                "sku": "WIRE-HAVELLS-2.5SQMM-90M",
                "description": "ISI marked copper electrical wire",
                "category_code": "ELECTRICAL",
                "unit_id": "unit-rolls",
                "tax_code_id": "tax-gst-18",
                "base_price": 2800.00,
                "cost_price": 2400.00,
                "attributes": {
                    "brand": "Havells",
                    "rating": "2.5 sq mm",
                    "color": "Red",
                    "length": "90 meters",
                    "warranty_months": "12"
                }
            }
            """
        )

        hardwareProducts.forEach { productJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", hardwareWorkspaceId)
                .body(productJson.trimIndent())
                .`when`()
                .post("/product/v1")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.status", equalTo("ACTIVE"))
        }
    }

    @Test
    @Order(5)
    @DisplayName("Step 4: Create comprehensive jewelry product catalog")
    fun `should create diverse jewelry products with precious metal specifications`() {
        val jewelryProducts = listOf(
            // Gold jewelry
            """
            {
                "name": "22K Gold Temple Necklace",
                "sku": "GOLD-NECKLACE-TEMPLE-001",
                "description": "Traditional temple design gold necklace with Lakshmi pendant",
                "category_code": "GOLD",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-3",
                "base_price": 125000.00,
                "cost_price": 118000.00,
                "attributes": {
                    "purity": "22K",
                    "weight_grams": "25.5",
                    "making_charges": "7000",
                    "design_type": "Temple",
                    "certification": "BIS Hallmark"
                }
            }
            """,
            """
            {
                "name": "22K Gold Bangles Pair",
                "sku": "GOLD-BANGLES-PAIR-001",
                "description": "Traditional gold bangles with intricate carving",
                "category_code": "GOLD",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-3",
                "base_price": 85000.00,
                "cost_price": 80000.00,
                "attributes": {
                    "purity": "22K",
                    "weight_grams": "18.2",
                    "making_charges": "5000",
                    "design_type": "Traditional",
                    "size": "2.6 inches"
                }
            }
            """,
            // Silver jewelry
            """
            {
                "name": "925 Silver Chain Necklace",
                "sku": "SILVER-CHAIN-925-20INCH",
                "description": "Sterling silver chain necklace 20 inch length",
                "category_code": "SILVER",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-12",
                "base_price": 3500.00,
                "cost_price": 2800.00,
                "attributes": {
                    "purity": "925 Sterling",
                    "weight_grams": "15.8",
                    "finish_type": "Rhodium Plated",
                    "length": "20 inches"
                }
            }
            """,
            // Diamond jewelry
            """
            {
                "name": "Diamond Solitaire Ring 0.5ct",
                "sku": "DIAMOND-RING-SOLITAIRE-05CT",
                "description": "18K white gold solitaire ring with 0.5ct diamond",
                "category_code": "DIAMOND",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-3",
                "base_price": 180000.00,
                "cost_price": 165000.00,
                "attributes": {
                    "gold_purity": "18K",
                    "diamond_carats": "0.50",
                    "diamond_clarity": "VS1",
                    "diamond_color": "F",
                    "certification": "GIA Certified"
                }
            }
            """,
            // Precious stones
            """
            {
                "name": "Natural Emerald 2.5ct",
                "sku": "STONE-EMERALD-25CT-COL",
                "description": "Natural Colombian emerald loose stone",
                "category_code": "STONES",
                "unit_id": "unit-pieces",
                "tax_code_id": "tax-gst-3",
                "base_price": 35000.00,
                "cost_price": 28000.00,
                "attributes": {
                    "stone_type": "Emerald",
                    "weight_carats": "2.5",
                    "origin": "Colombia",
                    "treatment": "Oil Treatment",
                    "certification": "Gubelin"
                }
            }
            """
        )

        jewelryProducts.forEach { productJson ->
            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", jewelryWorkspaceId)
                .body(productJson.trimIndent())
                .`when`()
                .post("/product/v1")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.status", equalTo("ACTIVE"))
        }
    }

    @Test
    @Order(6)
    @DisplayName("Step 5: Set inventory levels for all products across both businesses")
    fun `should set appropriate inventory levels for different business types`() {
        // Hardware inventory - higher volumes for construction materials
        val hardwareSkus = listOf(
            "CEMENT-ACC-GOLD-50KG" to 200.0,
            "CONCRETE-ULTRA-M25" to 50.0,
            "STEEL-TATA-TMT-12MM" to 500.0,
            "STEEL-ANGLE-40X40X5" to 100.0,
            "TOOL-STANLEY-HAMMER-450G" to 25.0,
            "WIRE-HAVELLS-2.5SQMM-90M" to 15.0
        )

        hardwareSkus.forEach { (sku, quantity) ->
            val inventoryRequest = """
                {
                    "adjustment_type": "SET",
                    "quantity": $quantity,
                    "reason": "Initial stock setup for hardware store",
                    "reorder_level": ${quantity * 0.2},
                    "max_stock_level": ${quantity * 3}
                }
            """.trimIndent()

            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", hardwareWorkspaceId)
                .body(inventoryRequest)
                .`when`()
                .put("/product/v1/$sku/inventory")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.current_stock", equalTo(quantity.toFloat()))
        }

        // Jewelry inventory - lower volumes for precious items
        val jewelrySkus = listOf(
            "GOLD-NECKLACE-TEMPLE-001" to 3.0,
            "GOLD-BANGLES-PAIR-001" to 5.0,
            "SILVER-CHAIN-925-20INCH" to 12.0,
            "DIAMOND-RING-SOLITAIRE-05CT" to 2.0,
            "STONE-EMERALD-25CT-COL" to 1.0
        )

        jewelrySkus.forEach { (sku, quantity) ->
            val inventoryRequest = """
                {
                    "adjustment_type": "SET",
                    "quantity": $quantity,
                    "reason": "Initial stock setup for jewelry store",
                    "reorder_level": 1.0,
                    "max_stock_level": ${quantity * 2}
                }
            """.trimIndent()

            RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $authToken")
                .header("X-Workspace-ID", jewelryWorkspaceId)
                .body(inventoryRequest)
                .`when`()
                .put("/product/v1/$sku/inventory")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.current_stock", equalTo(quantity.toFloat()))
        }
    }

    @Test
    @Order(7)
    @DisplayName("Step 6: Test product search and filtering capabilities")
    fun `should support comprehensive product search across different attributes`() {
        // Search by brand in hardware store
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .queryParam("search", "TATA")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(0)))
            .body("data.content[0].attributes.brand", equalTo("TATA"))

        // Search by category in jewelry store  
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("category_code", "GOLD")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(hasEntry("category_code", "GOLD")))

        // Price range filtering
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .queryParam("min_price", 500)
            .queryParam("max_price", 1000)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", everyItem(allOf(
                hasEntry("base_price", greaterThanOrEqualTo(500.0f)),
                hasEntry("base_price", lessThanOrEqualTo(1000.0f))
            )))

        // Low stock filtering
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("low_stock", true)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
    }

    @Test
    @Order(8)
    @DisplayName("Step 7: Test product inventory movements and tracking")
    fun `should track inventory movements with proper audit trail`() {
        val testSku = "CEMENT-ACC-GOLD-50KG"
        
        // Add stock - receiving from supplier
        val addStockRequest = """
            {
                "adjustment_type": "ADD",
                "quantity": 100.0,
                "reason": "Stock received from ACC supplier - Invoice #ACC2025090701"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .body(addStockRequest)
            .`when`()
            .put("/product/v1/$testSku/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.current_stock", equalTo(300.0f)) // 200 + 100
            .body("data.recent_movements[0].movement_type", equalTo("IN"))
            .body("data.recent_movements[0].quantity", equalTo(100.0f))

        // Subtract stock - damage/loss
        val subtractStockRequest = """
            {
                "adjustment_type": "SUBTRACT",
                "quantity": 25.0,
                "reason": "Damaged bags during transport - removed from inventory"
            }
        """.trimIndent()

        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .body(subtractStockRequest)
            .`when`()
            .put("/product/v1/$testSku/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.current_stock", equalTo(275.0f)) // 300 - 25
            .body("data.recent_movements[0].movement_type", equalTo("OUT"))
            .body("data.recent_movements[0].quantity", equalTo(25.0f))

        // Get movement history
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .queryParam("include_movements", true)
            .queryParam("movement_limit", 10)
            .`when`()
            .get("/product/v1/$testSku/inventory")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.recent_movements", hasSize(greaterThan(2))) // Initial SET + ADD + SUBTRACT
            .body("data.recent_movements[0].user_name", notNullValue())
            .body("data.recent_movements[0].timestamp", notNullValue())
    }

    @Test
    @Order(9)
    @DisplayName("Step 8: Test product catalog export and reporting")
    fun `should support product catalog export and analytics`() {
        // Export hardware catalog
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .queryParam("export_format", "CSV")
            .queryParam("include_inventory", true)
            .`when`()
            .get("/product/v1/list/export")
            .then()
            .statusCode(200)
            .contentType("text/csv")
            .header("Content-Disposition", containsString("products_export"))

        // Export jewelry catalog with precious metal details
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("export_format", "EXCEL")
            .queryParam("include_attributes", true)
            .queryParam("include_inventory", true)
            .`when`()
            .get("/product/v1/list/export")
            .then()
            .statusCode(200)
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

        // Get product analytics
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .queryParam("include_analytics", true)
            .`when`()
            .get("/product/v1/analytics")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.total_products", greaterThan(0))
            .body("data.total_inventory_value", greaterThan(0.0f))
            .body("data.category_distribution", notNullValue())
            .body("data.low_stock_products", greaterThanOrEqualTo(0))
    }

    @Test
    @Order(10)
    @DisplayName("Step 9: Test multi-tenant isolation in product catalogs")
    fun `should enforce strict multi-tenant isolation for product data`() {
        // Hardware store should not see jewelry products
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .queryParam("search", "gold")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(0)) // No gold products in hardware store

        // Jewelry store should not see hardware products
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("search", "cement")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(0)) // No cement products in jewelry store

        // Try to access jewelry product from hardware workspace
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .`when`()
            .get("/product/v1/GOLD-NECKLACE-TEMPLE-001")
            .then()
            .statusCode(404)
            .body("success", equalTo(false))
            .body("error.code", equalTo("PRODUCT_NOT_FOUND"))
    }

    @Test
    @Order(11)
    @DisplayName("Step 10: Validate complete product catalog functionality")
    fun `should have fully functional product catalogs for both business types`() {
        // Validate hardware catalog completeness
        val hardwareStats = RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .queryParam("include_stats", true)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(5)))
            .body("data.statistics.total_products", greaterThan(5))
            .body("data.statistics.categories_covered", hasItems("CEMENT", "STEEL", "TOOLS", "ELECTRICAL"))
            .extract()

        // Validate jewelry catalog completeness
        val jewelryStats = RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", jewelryWorkspaceId)
            .queryParam("include_stats", true)
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.content", hasSize(greaterThan(4)))
            .body("data.statistics.total_products", greaterThan(4))
            .body("data.statistics.categories_covered", hasItems("GOLD", "SILVER", "DIAMOND", "STONES"))
            .extract()

        // Validate search performance
        RestAssured
            .given()
            .header("Authorization", "Bearer $authToken")
            .header("X-Workspace-ID", hardwareWorkspaceId)
            .queryParam("performance_mode", true)
            .queryParam("search", "steel")
            .`when`()
            .get("/product/v1/list")
            .then()
            .statusCode(200)
            .time(lessThan(1000L)) // Should respond within 1 second
            .body("success", equalTo(true))
    }
}