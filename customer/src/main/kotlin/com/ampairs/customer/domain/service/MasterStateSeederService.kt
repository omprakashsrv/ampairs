package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.model.MasterState
import com.ampairs.customer.repository.MasterStateRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service to seed initial master states data on application startup.
 * Loads comprehensive state/country data for workspace imports.
 */
@Service
@Transactional
class MasterStateSeederService(
    private val masterStateRepository: MasterStateRepository
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(MasterStateSeederService::class.java)

    override fun run(vararg args: String?) {
        seedMasterStates()
    }

    fun seedMasterStates() {
        logger.info("Starting master states seeding process...")

        val existingStates = masterStateRepository.findAll().associateBy { it.stateCode }

        getIndianStates().forEach { stateData ->
            val existingState = existingStates[stateData.stateCode]
            if (existingState == null) {
                logger.info("Seeding new master state: {}", stateData.stateCode)
                masterStateRepository.save(stateData)
            } else {
                logger.debug("Updating existing master state: {}", stateData.stateCode)
                existingState.apply {
                    name = stateData.name
                    shortName = stateData.shortName
                    countryCode = stateData.countryCode
                    countryName = stateData.countryName
                    region = stateData.region
                    timezone = stateData.timezone
                    localName = stateData.localName
                    capital = stateData.capital
                    population = stateData.population
                    areaSqKm = stateData.areaSqKm
                    gstCode = stateData.gstCode
                    postalCodePattern = stateData.postalCodePattern
                    active = stateData.active
                }
                masterStateRepository.save(existingState)
            }
        }

        // Add other countries data
        getOtherCountriesStates().forEach { stateData ->
            val existingState = existingStates[stateData.stateCode]
            if (existingState == null) {
                logger.info("Seeding new master state: {}", stateData.stateCode)
                masterStateRepository.save(stateData)
            }
        }

        logger.info("Master states seeding completed")
    }

    /**
     * Get comprehensive Indian states and union territories data
     */
    private fun getIndianStates(): List<MasterState> {
        return listOf(
            createIndianState("IN-AP", "Andhra Pradesh", "AP", "Southern India", "Amaravati", 49386799L, 162968.0, "28", "5[0-3]\\d{4}"),
            createIndianState("IN-AR", "Arunachal Pradesh", "AR", "Northeastern India", "Itanagar", 1382611L, 83743.0, "12", "7[89]\\d{4}"),
            createIndianState("IN-AS", "Assam", "AS", "Northeastern India", "Dispur", 31205576L, 78438.0, "18", "78\\d{4}"),
            createIndianState("IN-BR", "Bihar", "BR", "Eastern India", "Patna", 104099452L, 94163.0, "10", "8[0-5]\\d{4}"),
            createIndianState("IN-CT", "Chhattisgarh", "CG", "Central India", "Raipur", 25545198L, 135192.0, "22", "49\\d{4}"),
            createIndianState("IN-GA", "Goa", "GA", "Western India", "Panaji", 1458545L, 3702.0, "30", "40[34]\\d{3}"),
            createIndianState("IN-GJ", "Gujarat", "GJ", "Western India", "Gandhinagar", 60439692L, 196244.0, "24", "[2-3]\\d{5}"),
            createIndianState("IN-HR", "Haryana", "HR", "Northern India", "Chandigarh", 25351462L, 44212.0, "06", "1[2-3]\\d{4}"),
            createIndianState("IN-HP", "Himachal Pradesh", "HP", "Northern India", "Shimla", 6864602L, 55673.0, "02", "1[7-9]\\d{4}"),
            createIndianState("IN-JK", "Jammu and Kashmir", "JK", "Northern India", "Srinagar", 12267032L, 222236.0, "01", "(1[89]|19)\\d{4}"),
            createIndianState("IN-JH", "Jharkhand", "JH", "Eastern India", "Ranchi", 32988134L, 79716.0, "20", "8[1-3]\\d{4}"),
            createIndianState("IN-KA", "Karnataka", "KA", "Southern India", "Bengaluru", 61095297L, 191791.0, "29", "[5-6]\\d{5}"),
            createIndianState("IN-KL", "Kerala", "KL", "Southern India", "Thiruvananthapuram", 33406061L, 38852.0, "32", "[6-7]\\d{5}"),
            createIndianState("IN-MP", "Madhya Pradesh", "MP", "Central India", "Bhopal", 72626809L, 308252.0, "23", "[4-5]\\d{5}"),
            createIndianState("IN-MH", "Maharashtra", "MH", "Western India", "Mumbai", 112374333L, 307713.0, "27", "[4-4]\\d{5}"),
            createIndianState("IN-MN", "Manipur", "MN", "Northeastern India", "Imphal", 2570390L, 22327.0, "14", "79[05-8]\\d{3}"),
            createIndianState("IN-ML", "Meghalaya", "ML", "Northeastern India", "Shillong", 2966889L, 22429.0, "17", "79[3-4]\\d{3}"),
            createIndianState("IN-MZ", "Mizoram", "MZ", "Northeastern India", "Aizawl", 1097206L, 21081.0, "15", "796\\d{3}"),
            createIndianState("IN-NL", "Nagaland", "NL", "Northeastern India", "Kohima", 1978502L, 16579.0, "13", "7[9-8]\\d{4}"),
            createIndianState("IN-OR", "Odisha", "OD", "Eastern India", "Bhubaneswar", 42350000L, 155707.0, "21", "7[5-7]\\d{4}"),
            createIndianState("IN-PB", "Punjab", "PB", "Northern India", "Chandigarh", 27743338L, 50362.0, "03", "1[4-6]\\d{4}"),
            createIndianState("IN-RJ", "Rajasthan", "RJ", "Northern India", "Jaipur", 68548437L, 342239.0, "08", "[3-4]\\d{5}"),
            createIndianState("IN-SK", "Sikkim", "SK", "Northeastern India", "Gangtok", 610577L, 7096.0, "11", "737\\d{3}"),
            createIndianState("IN-TN", "Tamil Nadu", "TN", "Southern India", "Chennai", 72147030L, 130060.0, "33", "[6-6]\\d{5}"),
            createIndianState("IN-TG", "Telangana", "TS", "Southern India", "Hyderabad", 35193978L, 112077.0, "36", "[5-5]\\d{5}"),
            createIndianState("IN-TR", "Tripura", "TR", "Northeastern India", "Agartala", 3673917L, 10486.0, "16", "79[9-9]\\d{3}"),
            createIndianState("IN-UP", "Uttar Pradesh", "UP", "Northern India", "Lucknow", 199812341L, 240928.0, "09", "[2-3]\\d{5}"),
            createIndianState("IN-UT", "Uttarakhand", "UK", "Northern India", "Dehradun", 10086292L, 53483.0, "05", "[2-3]\\d{5}"),
            createIndianState("IN-WB", "West Bengal", "WB", "Eastern India", "Kolkata", 91276115L, 88752.0, "19", "[7-7]\\d{5}"),

            // Union Territories
            createIndianState("IN-AN", "Andaman and Nicobar Islands", "AN", "Island Territory", "Port Blair", 380581L, 8249.0, "35", "744\\d{3}").apply {
                metadata = """{"type": "union_territory"}"""
            },
            createIndianState("IN-CH", "Chandigarh", "CH", "Northern India", "Chandigarh", 1055450L, 114.0, "04", "16[0-1]\\d{3}").apply {
                metadata = """{"type": "union_territory"}"""
            },
            createIndianState("IN-DN", "Dadra and Nagar Haveli and Daman and Diu", "DD", "Western India", "Daman", 585764L, 603.0, "26", "39[6-9]\\d{3}").apply {
                metadata = """{"type": "union_territory"}"""
            },
            createIndianState("IN-DL", "Delhi", "DL", "Northern India", "New Delhi", 32820000L, 1484.0, "07", "1[1-1]\\d{4}").apply {
                metadata = """{"type": "union_territory"}"""
            },
            createIndianState("IN-LA", "Ladakh", "LA", "Northern India", "Leh", 290492L, 96701.0, "02", "1[9-9]\\d{4}").apply {
                metadata = """{"type": "union_territory"}"""
            },
            createIndianState("IN-LD", "Lakshadweep", "LD", "Island Territory", "Kavaratti", 64473L, 32.0, "31", "682\\d{3}").apply {
                metadata = """{"type": "union_territory"}"""
            },
            createIndianState("IN-PY", "Puducherry", "PY", "Southern India", "Puducherry", 1247953L, 492.0, "34", "60[5-9]\\d{3}").apply {
                metadata = """{"type": "union_territory"}"""
            }
        )
    }

    /**
     * Get states/provinces for other major countries
     */
    private fun getOtherCountriesStates(): List<MasterState> {
        return listOf(
            // United States - Major states
            createState("US-CA", "California", "CA", "US", "United States", "Pacific Coast", "America/Los_Angeles", "California", "Sacramento", 39538223L, 423967.0, "9[0-6]\\d{3}"),
            createState("US-NY", "New York", "NY", "US", "United States", "Northeast", "America/New_York", "New York", "Albany", 19336776L, 141297.0, "1[0-1]\\d{3}"),
            createState("US-TX", "Texas", "TX", "US", "United States", "South Central", "America/Chicago", "Texas", "Austin", 29360759L, 695662.0, "7[3-9]\\d{3}"),
            createState("US-FL", "Florida", "FL", "US", "United States", "Southeast", "America/New_York", "Florida", "Tallahassee", 21733312L, 170312.0, "3[2-4]\\d{3}"),

            // United Kingdom
            createState("GB-ENG", "England", "ENG", "GB", "United Kingdom", "Great Britain", "Europe/London", "England", "London", 56550138L, 130279.0, "[A-Z]{1,2}[0-9][A-Z0-9]? [0-9][A-Z]{2}"),
            createState("GB-SCT", "Scotland", "SCT", "GB", "United Kingdom", "Great Britain", "Europe/London", "Scotland", "Edinburgh", 5466000L, 77933.0, "[A-Z]{1,2}[0-9][A-Z0-9]? [0-9][A-Z]{2}"),
            createState("GB-WLS", "Wales", "WLS", "GB", "United Kingdom", "Great Britain", "Europe/London", "Wales", "Cardiff", 3169586L, 20779.0, "[A-Z]{1,2}[0-9][A-Z0-9]? [0-9][A-Z]{2}"),
            createState("GB-NIR", "Northern Ireland", "NIR", "GB", "United Kingdom", "Ireland", "Europe/London", "Northern Ireland", "Belfast", 1895510L, 14130.0, "BT[0-9]{1,2} [0-9][A-Z]{2}"),

            // Canada - Major provinces
            createState("CA-ON", "Ontario", "ON", "CA", "Canada", "Central Canada", "America/Toronto", "Ontario", "Toronto", 14826276L, 1076395.0, "[A-Z][0-9][A-Z] [0-9][A-Z][0-9]"),
            createState("CA-QC", "Quebec", "QC", "CA", "Canada", "Central Canada", "America/Toronto", "Québec", "Quebec City", 8604495L, 1542056.0, "[A-Z][0-9][A-Z] [0-9][A-Z][0-9]"),
            createState("CA-BC", "British Columbia", "BC", "CA", "Canada", "Western Canada", "America/Vancouver", "British Columbia", "Victoria", 5214805L, 944735.0, "[A-Z][0-9][A-Z] [0-9][A-Z][0-9]"),

            // Australia - Major states
            createState("AU-NSW", "New South Wales", "NSW", "AU", "Australia", "Eastern Australia", "Australia/Sydney", "New South Wales", "Sydney", 8176368L, 809444.0, "[0-9]{4}"),
            createState("AU-VIC", "Victoria", "VIC", "AU", "Australia", "Eastern Australia", "Australia/Melbourne", "Victoria", "Melbourne", 6694000L, 237657.0, "[0-9]{4}"),
            createState("AU-QLD", "Queensland", "QLD", "AU", "Australia", "Eastern Australia", "Australia/Brisbane", "Queensland", "Brisbane", 5206400L, 1851736.0, "[0-9]{4}"),
        )
    }

    /**
     * Helper to create Indian state
     */
    private fun createIndianState(
        stateCode: String,
        name: String,
        shortName: String,
        region: String,
        capital: String,
        population: Long,
        areaSqKm: Double,
        gstCode: String,
        postalPattern: String
    ): MasterState {
        return MasterState().apply {
            this.stateCode = stateCode
            this.name = name
            this.shortName = shortName
            this.countryCode = "IN"
            this.countryName = "India"
            this.region = region
            this.timezone = "Asia/Kolkata"
            this.capital = capital
            this.population = population
            this.areaSqKm = areaSqKm
            this.gstCode = gstCode
            this.postalCodePattern = postalPattern
            this.active = true
        }
    }


    /**
     * Helper to create state for other countries
     */
    private fun createState(
        stateCode: String,
        name: String,
        shortName: String,
        countryCode: String,
        countryName: String,
        region: String,
        timezone: String,
        localName: String,
        capital: String,
        population: Long,
        areaSqKm: Double,
        postalPattern: String
    ): MasterState {
        return MasterState().apply {
            this.stateCode = stateCode
            this.name = name
            this.shortName = shortName
            this.countryCode = countryCode
            this.countryName = countryName
            this.region = region
            this.timezone = timezone
            this.localName = localName
            this.capital = capital
            this.population = population
            this.areaSqKm = areaSqKm
            this.postalCodePattern = postalPattern
            this.active = true
        }
    }
}