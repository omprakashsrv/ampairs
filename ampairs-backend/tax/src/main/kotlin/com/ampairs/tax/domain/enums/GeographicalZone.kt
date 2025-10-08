package com.ampairs.tax.domain.enums

enum class GeographicalZone(val displayName: String, val stateCodes: List<String>) {
    NORTH(
        "North India",
        listOf("DL", "HR", "HP", "JK", "PB", "RJ", "UP", "UK", "CH")
    ),
    SOUTH(
        "South India",
        listOf("AP", "TG", "KA", "KL", "TN", "PY")
    ),
    EAST(
        "East India",
        listOf("WB", "BH", "JH", "OR", "SK", "AS", "AR", "MN", "MZ", "NL", "TR", "ML", "MG")
    ),
    WEST(
        "West India",
        listOf("MH", "GJ", "MP", "CG", "GA", "DN", "DD")
    ),
    UNION_TERRITORIES(
        "Union Territories",
        listOf("AN", "CH", "DN", "DD", "DL", "JK", "LA", "LD", "PY")
    ),
    ALL_INDIA(
        "All India",
        emptyList()
    );

    companion object {
        fun getZoneByStateCode(stateCode: String): GeographicalZone? {
            return values().find { zone ->
                zone.stateCodes.contains(stateCode.uppercase())
            }
        }

        fun isUnionTerritory(stateCode: String): Boolean {
            return UNION_TERRITORIES.stateCodes.contains(stateCode.uppercase())
        }
    }
}