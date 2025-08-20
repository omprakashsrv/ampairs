package com.ampairs.auth

import AuthRoute
import Route
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.domain.LoginStatus
import com.ampairs.auth.ui.LoginScope
import com.ampairs.auth.ui.LoginScreen
import com.ampairs.auth.ui.OtpScreen
import com.ampairs.auth.ui.PhoneScreen
import com.ampairs.auth.ui.UserUpdateScreen
import org.koin.core.context.GlobalContext

fun NavGraphBuilder.authNavigation(navigator: NavController, onLoginSuccess: () -> Unit) {

    navigation<Route.Login>(startDestination = AuthRoute.LoginRoot) {

        val loginScope = GlobalContext.get().createScope<LoginScope>()
        val tokenRepository = GlobalContext.get().get<TokenRepository>()
        
        composable<AuthRoute.LoginRoot> {
            LoginScreen(loginScope) { loginStatus, userEntity ->
                if (loginStatus == LoginStatus.LOGGED_IN) {
                    // Check if user's first name is empty, then navigate to UserUpdate screen
                    if (userEntity?.first_name.isNullOrBlank()) {
                        navigator.navigate(AuthRoute.UserUpdate)
                    } else {
                        loginScope.close()
                        // Check if user has selected a workspace
                        val hasSelectedWorkspace = tokenRepository.getCompanyId().isNotBlank()
                        if (hasSelectedWorkspace) {
                            // User has selected workspace, go to main app
                            onLoginSuccess()
                        } else {
                            // User needs to select workspace, go to workspace selection
                            navigator.navigate(Route.Workspace) {
                                popUpTo(Route.Login) { inclusive = true }
                            }
                        }
                    }
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
                navigator.navigate(AuthRoute.UserUpdate)
            }
        }
        composable<AuthRoute.UserUpdate> {
            UserUpdateScreen {
                loginScope.close()
                // After user update, check if workspace is selected
                val hasSelectedWorkspace = tokenRepository.getCompanyId().isNotBlank()
                if (hasSelectedWorkspace) {
                    // User has selected workspace, go to main app
                    onLoginSuccess()
                } else {
                    // User needs to select workspace, go to workspace selection
                    navigator.navigate(Route.Workspace) {
                        popUpTo(Route.Login) { inclusive = true }
                    }
                }
            }
        }
    }
}