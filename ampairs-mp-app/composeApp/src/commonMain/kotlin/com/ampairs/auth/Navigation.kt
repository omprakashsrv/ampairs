package com.ampairs.auth

import AuthRoute
import Route
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import androidx.navigation.navigation
import com.ampairs.auth.domain.LoginStatus
import com.ampairs.auth.ui.LoginScope
import com.ampairs.auth.ui.LoginScreen
import com.ampairs.auth.ui.OtpScreen
import com.ampairs.auth.ui.PhoneScreen
import org.koin.core.context.GlobalContext

fun NavGraphBuilder.authNavigation(navigator: NavController, onLoginSuccess: () -> Unit) {

    navigation<Route.Login>(startDestination = AuthRoute.LoginRoot) {

        val loginScope = GlobalContext.get().createScope<LoginScope>()
        composable<AuthRoute.LoginRoot> {
            LoginScreen(loginScope) {
                if (it == LoginStatus.LOGGED_IN) {
                    loginScope.close()
                    onLoginSuccess()
                } else {
                    navigator.navigate(AuthRoute.Phone)
                }
            }
        }
        composable<AuthRoute.Phone> {
            PhoneScreen(loginScope) {
                navigator.navigate(AuthRoute.Otp)
            }
        }
        composable<AuthRoute.Otp> {
            OtpScreen(scope = loginScope) {
                val options = navOptions {
                    popUpTo<AuthRoute.LoginRoot> {
                        this.inclusive = true
                    }
                    launchSingleTop = true // Avoid multiple instances of the same destination
                }
                navigator.navigate(AuthRoute.LoginRoot, navOptions = options)
            }
        }
    }
}