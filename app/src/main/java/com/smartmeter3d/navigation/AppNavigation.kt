package com.smartmeter3d.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartmeter3d.ui.screens.ComingSoonScreen
import com.smartmeter3d.ui.screens.HomeScreen
import com.smartmeter3d.ui.screens.PhotoMeasureScreen
import com.smartmeter3d.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PHOTO = "photo"
    const val COMING_SOON = "coming_soon/{titleRes}"
    fun comingSoon(titleRes: Int) = "coming_soon/$titleRes"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(onOpen = { nav.navigate(it) }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PHOTO) { PhotoMeasureScreen(onBack = { nav.popBackStack() }) }
        composable(
            Routes.COMING_SOON,
            arguments = listOf(navArgument("titleRes") { type = NavType.IntType })
        ) { entry ->
            ComingSoonScreen(
                titleRes = entry.arguments?.getInt("titleRes") ?: 0,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
