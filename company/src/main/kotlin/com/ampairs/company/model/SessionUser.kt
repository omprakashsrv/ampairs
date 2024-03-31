package com.ampairs.company.model

import com.ampairs.user.model.User


class SessionUser(val company: Company) : User() {
    constructor(user: User, company: Company) : this(company) {
        this.firstName = user.firstName
        this.lastName = user.lastName
        this.email = user.email
        this.countryCode = user.countryCode
        this.phone = user.phone
        this.active = user.active
    }
}