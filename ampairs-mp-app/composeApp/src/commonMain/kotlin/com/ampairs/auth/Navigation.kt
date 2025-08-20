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
import com.ampairs.auth.ui.UserSelectionScreen
import com.ampairs.auth.ui.UserUpdateScreen
import org.koin.core.context.GlobalContext

fun NavGraphBuilder.authNavigation(navigator: NavController, onLoginSuccess: () -> Unit) {

    navigation<Route.Login>(startDestination = AuthRoute.UserSelection) {

        val loginScope = GlobalContext.get().createScope<LoginScope>()
        val tokenRepository = GlobalContext.get().get<TokenRepository>()
        
        composable<AuthRoute.UserSelection> {
            UserSelectionScreen(
                onUserSelected = { userId ->
                    // Set the selected user as current and check their state
                    kotlinx.coroutines.runBlocking {
                        tokenRepository.setCurrentUser(userId)
                        val hasSelectedWorkspace = tokenRepository.getCompanyIdForUser(userId).isNotBlank()
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
                },
                onAddNewUser = {
                    navigator.navigate(AuthRoute.LoginRoot)
                },
                onNoUsers = {
                    // If no users, go directly to login
                    navigator.navigate(AuthRoute.LoginRoot) {
                        popUpTo(AuthRoute.UserSelection) { inclusive = true }
                    }
                }
            )
        }
        
        composable<AuthRoute.LoginRoot> {
            LoginScreen(loginScope) { loginStatus, userEntity ->
                if (loginStatus == LoginStatus.LOGGED_IN) {
                    // Check if user's first name is empty, then navigate to UserUpdate screen
                    if (userEntity?.first_name.isNullOrBlank()) {
                        navigator.navigate(AuthRoute.UserUpdate)
                    } else {
                        loginScope.close()
                        
                        kotlinx.coroutines.runBlocking {
                            // Add user to multi-user system if they have tokens
                            val accessToken = tokenRepository.getAccessToken()
                            val refreshToken = tokenRepository.getRefreshToken()
                            
                            if (userEntity != null && !accessToken.isNullOrBlank()) {
                                tokenRepository.addAuthenticatedUser(
                                    userId = userEntity.id,
                                    accessToken = accessToken,
                                    refreshToken = refreshToken
                                )
                                tokenRepository.setCurrentUser(userEntity.id)
                            }
                            
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
                
                kotlinx.coroutines.runBlocking {
                    // Add user to multi-user system after profile update
                    val accessToken = tokenRepository.getAccessToken()
                    val refreshToken = tokenRepository.getRefreshToken()
                    
                    // Get current user after update (should be available from the UserUpdateScreen)
                    if (!accessToken.isNullOrBlank()) {
                        val currentUserId = tokenRepository.getCurrentUserId()
                        if (currentUserId != null) {
                            tokenRepository.addAuthenticatedUser(
                                userId = currentUserId,
                                accessToken = accessToken,
                                refreshToken = refreshToken
                            )
                        }
                    }
                    
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
}