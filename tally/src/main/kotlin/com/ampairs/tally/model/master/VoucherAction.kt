package com.ampairs.tally.model.master

/**
 * Created by paripranu on 05/11/18.
 */
enum class VoucherAction(private val action: String) {
    Create("Create"),
    Alter("Alter"),
    Cancel("Cancel"),
    Delete("Delete")
}
