package com.ampairs.auth.domain.dto

import com.ampairs.auth.domain.model.User
import com.fasterxml.jackson.annotation.JsonIgnore

class UserResponse(@JsonIgnore val user: User) {
    var id: String = user.id
    var firstName: String = user.firstName
    var lastName: String? = user.lastName
    var countryCode: Int = user.countryCode
    var phone: String = user.phone
}