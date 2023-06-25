package com.ampairs.core.domain.dto


class SuccessResponse<T> {
    var response: T
    var message: String? = null

    constructor(response: T) {
        this.response = response
    }

    constructor(response: T, message: String) {
        this.response = response
        this.message = message
    }

}