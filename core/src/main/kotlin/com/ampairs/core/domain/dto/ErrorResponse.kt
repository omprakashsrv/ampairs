package com.ampairs.core.domain.dto

import com.fasterxml.jackson.annotation.JsonInclude


@JsonInclude(JsonInclude.Include.NON_NULL)
class ErrorResponse(code: Int, message: String) {
    val error: Error = Error(code, message)
}

