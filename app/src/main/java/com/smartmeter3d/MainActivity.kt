package com.smartmeter3d

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.smartmeter3d.navigation.AppNavigation
import com.smartmeter3d.ui.theme.SmartMeterTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartMeterTheme {
                AppNavigation()
            }
        }
    }
}
