package com.ampairs.core.domain.dto

import com.ampairs.core.domain.model.User
import com.fasterxml.jackson.annotation.JsonIgnore

class UserResponse(@JsonIgnore val user: User) {
    var id: String = user.id
    var firstName: String = user.firstName
    var lastName: String? = user.lastName
    var userName: String? = user.userName
    var countryCode: Int = user.countryCode
    var phone: String = user.phone
}