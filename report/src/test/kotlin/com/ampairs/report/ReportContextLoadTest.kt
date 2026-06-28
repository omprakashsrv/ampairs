package com.ampairs.report

import com.ampairs.AmpairsApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Boots the full application context on the H2 `test` profile (the same setup the
 * auth/customer/ecom integration tests use) to guarantee the `report` module wires
 * cleanly into `AmpairsApplication`. Regression guard for context-load failures
 * (e.g. autowiring a bean that isn't available).
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
class ReportContextLoadTest {

    @Test
    fun contextLoads() {
    }
}
