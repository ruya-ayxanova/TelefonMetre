package com.smartmeter3d.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartmeter3d.database.MeasurementType
import com.smartmeter3d.ui.screens.ArMeasureScreen
import com.smartmeter3d.ui.screens.CalculatorScreen
import com.smartmeter3d.ui.screens.HomeScreen
import com.smartmeter3d.ui.screens.ManualMeasureScreen
import com.smartmeter3d.ui.screens.PhotoMeasureScreen
import com.smartmeter3d.ui.screens.ProjectsScreen
import com.smartmeter3d.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PHOTO = "photo"
    const val AR = "ar"
    const val CALCULATOR = "calculator"
    const val PROJECTS = "projects"
    const val MANUAL = "manual/{kind}"
    fun manual(type: MeasurementType) = "manual/${type.name}"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(onOpen = { nav.navigate(it) }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PHOTO) { PhotoMeasureScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.AR) { ArMeasureScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.CALCULATOR) { CalculatorScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PROJECTS) { ProjectsScreen(onBack = { nav.popBackStack() }) }
        composable(
            Routes.MANUAL,
            arguments = listOf(navArgument("kind") { type = NavType.StringType })
        ) { entry ->
            val name = entry.arguments?.getString("kind")
            val measurementType = MeasurementType.values().firstOrNull { it.name == name } ?: MeasurementType.CUSTOM
            ManualMeasureScreen(
                type = measurementType,
                onBack = { nav.popBackStack() },
                onOpenAr = { nav.navigate(Routes.AR) },
                onOpenPhoto = { nav.navigate(Routes.PHOTO) }
            )
        }
    }
}
