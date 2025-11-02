package com.ampairs.core.domain.dto


class SuccessResponse<T> {
    var response: T

    constructor(response: T) {
        this.response = response
    }

}