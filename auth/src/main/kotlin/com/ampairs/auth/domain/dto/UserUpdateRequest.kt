package com.ampairs.auth.domain.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

class UserUpdateRequest {

    @NotNull
    @NotEmpty
    var firstName: String = ""

    @NotNull
    var lastName: String = ""
}