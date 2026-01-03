package com.ampairs.inventory.config

/**
 * Constants for the Inventory Module
 *
 * Defines all sequence ID prefixes, transaction types, reasons, status codes,
 * and stock consumption strategies used throughout the inventory management system.
 */
interface Constants {
    companion object {
        // Standard ID length for all entities
        const val ID_LENGTH = 34

        // Entity Sequence ID Prefixes
        const val WAREHOUSE_PREFIX = "WHR"
        const val INVENTORY_ITEM_PREFIX = "INV"
        const val TRANSACTION_PREFIX = "TXN"
        const val BATCH_PREFIX = "BCH"
        const val SERIAL_PREFIX = "SRL"
        const val LEDGER_PREFIX = "LDG"
        const val CONFIG_PREFIX = "CFG"

        // Transaction Types
        const val TXN_TYPE_STOCK_IN = "STOCK_IN"
        const val TXN_TYPE_STOCK_OUT = "STOCK_OUT"
        const val TXN_TYPE_TRANSFER = "TRANSFER"
        const val TXN_TYPE_ADJUSTMENT = "ADJUSTMENT"
        const val TXN_TYPE_COUNT = "COUNT"

        // Transaction Reasons
        const val REASON_PURCHASE = "PURCHASE"
        const val REASON_SALE = "SALE"
        const val REASON_RETURN = "RETURN"
        const val REASON_DAMAGE = "DAMAGE"
        const val REASON_LOSS = "LOSS"
        const val REASON_THEFT = "THEFT"
        const val REASON_OPENING = "OPENING"
        const val REASON_CORRECTION = "CORRECTION"
        const val REASON_TRANSFER = "TRANSFER"
        const val REASON_COUNT_ADJUSTMENT = "COUNT_ADJUSTMENT"
        const val REASON_EXPIRED = "EXPIRED"
        const val REASON_PRODUCTION = "PRODUCTION"
        const val REASON_CONSUMPTION = "CONSUMPTION"

        // Serial Number Status
        const val SERIAL_AVAILABLE = "AVAILABLE"
        const val SERIAL_RESERVED = "RESERVED"
        const val SERIAL_SOLD = "SOLD"
        const val SERIAL_DAMAGED = "DAMAGED"
        const val SERIAL_RETURNED = "RETURNED"
        const val SERIAL_WARRANTY_CLAIM = "WARRANTY_CLAIM"

        // Stock Consumption Strategies
        const val STRATEGY_FIFO = "FIFO"       // First In First Out
        const val STRATEGY_FEFO = "FEFO"       // First Expiry First Out
        const val STRATEGY_LIFO = "LIFO"       // Last In First Out
        const val STRATEGY_MANUAL = "MANUAL"   // Manual selection

        // Warehouse Types
        const val WAREHOUSE_TYPE_WAREHOUSE = "WAREHOUSE"
        const val WAREHOUSE_TYPE_STORE = "STORE"
        const val WAREHOUSE_TYPE_GODOWN = "GODOWN"
        const val WAREHOUSE_TYPE_SHOWROOM = "SHOWROOM"
        const val WAREHOUSE_TYPE_FACTORY = "FACTORY"

        // Reference Types
        const val REF_TYPE_ORDER = "ORDER"
        const val REF_TYPE_INVOICE = "INVOICE"
        const val REF_TYPE_PURCHASE = "PURCHASE"
        const val REF_TYPE_PRODUCTION = "PRODUCTION"
        const val REF_TYPE_COUNT = "COUNT"
        const val REF_TYPE_MANUAL = "MANUAL"

        // Error Messages
        const val ERROR_WAREHOUSE_NOT_FOUND = "Warehouse not found"
        const val ERROR_INVENTORY_ITEM_NOT_FOUND = "Inventory item not found"
        const val ERROR_INSUFFICIENT_STOCK = "Insufficient stock available"
        const val ERROR_BATCH_NOT_FOUND = "Batch not found"
        const val ERROR_SERIAL_NOT_FOUND = "Serial number not found"
        const val ERROR_DUPLICATE_SERIAL = "Duplicate serial number"
        const val ERROR_INVALID_TRANSACTION = "Invalid transaction"
        const val ERROR_NEGATIVE_STOCK_NOT_ALLOWED = "Negative stock not allowed"
        const val ERROR_DUPLICATE_WAREHOUSE_CODE = "Warehouse code already exists"
        const val ERROR_WAREHOUSE_HAS_INVENTORY = "Cannot delete warehouse with existing inventory"
    }
}
