package com.ampairs.tally.client

import com.ampairs.tally.model.master.*
import com.ampairs.tally.model.voucher.Voucher

interface TallyClient {
    fun getCompanies(): List<String>
    fun getLedgers(): List<Ledger>
    fun getStockItems(): List<StockItem>
    fun getVouchers(): List<Voucher>
    fun sendRequest(request: String): String
    fun isConnected(): Boolean
}