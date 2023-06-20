package com.ampairs.auth.domain.dto

import lombok.AllArgsConstructor
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
open class GenericSuccessResponse {
    var success = true
    var message: String? = null
}
