package io.soft.imagenee.presentation._structure

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.soft.imagenee.presentation.authorize.AuthorizeScreen
import io.soft.imagenee.presentation.main.MainScreen
import io.soft.imagenee.presentation.main.single.ShowImageScreen
import io.soft.imagenee.presentation.welcome.WelcomeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageneeNavHost(navHostController: NavHostController, onBoarding: Boolean) {
    val startDestination = if (onBoarding) Destinations.welcome else Destinations.main

    NavHost(
        navController = navHostController,
        startDestination = startDestination
    ) {
        composable(route = Destinations.welcome) {
            WelcomeScreen(
                onLogin = { navHostController.navigate(Destinations.login) },
                onSignUp = { navHostController.navigate(Destinations.signup) }
            )
        }

        composable(route = Destinations.login) {
            AuthorizeScreen(
                onBack = navHostController::popBackStack,
                isSignIn = true,
                onAuthorizeSuccess = {
                    navHostController.navigate(Destinations.main) {
                        popUpTo(Destinations.main) {
                            inclusive = true
                            saveState = false
                        }
                    }
                }
            )
        }

        composable(route = Destinations.signup) {
            AuthorizeScreen(
                onBack = navHostController::popBackStack,
                isSignIn = false,
                onAuthorizeSuccess = {
                    navHostController.navigate(Destinations.main)
                }
            )
        }

        composable(route = Destinations.main) {
            MainScreen(
                onOpenImage = {
                    navHostController.navigate("${Destinations.show_image}/" + it.id + "/" + it.name + "/" + it.image)
                },
                onLogOut = {
                    navHostController.navigate(Destinations.welcome) {
                        popUpTo(Destinations.welcome) {
                            inclusive = true
                            saveState = false
                        }
                    }
                }
            )
        }

        composable(
            route = "${Destinations.show_image}/{id}/{name}/{image}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("image") { type = NavType.StringType }
            )
        ) { backStack ->
            ShowImageScreen(
                imageId = backStack.arguments?.getString("id", "") ?: "",
                imageName = backStack.arguments?.getString("name", "") ?: "",
                imagePath = backStack.arguments?.getString("image", "") ?: "",
                onBack = navHostController::popBackStack
            )
        }
    }
}