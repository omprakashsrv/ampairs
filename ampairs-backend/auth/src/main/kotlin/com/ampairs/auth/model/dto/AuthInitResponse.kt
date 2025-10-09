package com.ampairs.auth.model.dto

import com.ampairs.core.domain.dto.GenericSuccessResponse

class AuthInitResponse : GenericSuccessResponse() {

    var sessionId: String? = null

}
