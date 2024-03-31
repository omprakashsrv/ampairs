package com.ampairs.company.model

import com.ampairs.user.model.User


class SessionUser(val company: Company, val userCompany: UserCompany) : User() {
    constructor(user: User, userCompany: UserCompany) :
            this(userCompany.company, userCompany) {
        this.firstName = user.firstName
        this.lastName = user.lastName
        this.email = user.email
        this.countryCode = user.countryCode
        this.phone = user.phone
        this.active = user.active
    }
}