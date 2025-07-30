package com.ampairs.workspace.model

import com.ampairs.user.model.User


class SessionUser(val company: Workspace, val userWorkspace: UserWorkspace) : User() {
    constructor(user: User, userWorkspace: UserWorkspace) :
            this(userWorkspace.company, userWorkspace) {
        this.firstName = user.firstName
        this.lastName = user.lastName
        this.email = user.email
        this.countryCode = user.countryCode
        this.phone = user.phone
        this.active = user.active
    }
}