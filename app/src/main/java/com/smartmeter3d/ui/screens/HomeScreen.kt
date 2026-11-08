package com.smartmeter3d.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartmeter3d.R
import com.smartmeter3d.navigation.Routes
import com.smartmeter3d.ui.components.MenuCard

private data class MenuItem(val id: String, @StringRes val title: Int, val icon: ImageVector)

private val menuItems = listOf(
    MenuItem("ar", R.string.menu_ar, Icons.Filled.ViewInAr),
    MenuItem("photo", R.string.menu_photo, Icons.Filled.PhotoCamera),
    MenuItem("wall", R.string.menu_wall, Icons.Filled.CropPortrait),
    MenuItem("closet", R.string.menu_closet, Icons.Filled.Checkroom),
    MenuItem("door", R.string.menu_door, Icons.Filled.DoorFront),
    MenuItem("window", R.string.menu_window, Icons.Filled.Window),
    MenuItem("room", R.string.menu_room, Icons.Filled.MeetingRoom),
    MenuItem("calculator", R.string.menu_calculator, Icons.Filled.Calculate),
    MenuItem("projects", R.string.menu_projects, Icons.Filled.Folder),
    MenuItem("settings", R.string.menu_settings, Icons.Filled.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.home_title), style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { inner ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(menuItems, key = { it.id }) { item ->
                MenuCard(
                    title = stringResource(item.title),
                    icon = item.icon,
                    onClick = {
                        onOpen(if (item.id == "settings") Routes.SETTINGS else Routes.comingSoon(item.title))
                    }
                )
            }
        }
    }
}
