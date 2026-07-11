package com.iamsubho.drivesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.iamsubho.drivesync.presentation.navigation.AppNavGraph
import com.iamsubho.drivesync.presentation.theme.DriveSyncTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DriveSyncTheme {
                AppNavGraph(navController = rememberNavController())
            }
        }
    }
}
