package com.ampairs.menu

import androidx.navigation.NavController

class AppNavigator {

    var navigator: NavController? = null

    fun navigate(route: String) {
        navigator?.navigate(route)
    }

    fun goBack() {
        navigator?.popBackStack()
    }

}