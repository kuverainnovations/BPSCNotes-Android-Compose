package com.example.bpscnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.bpscnotes.core.ui.t.BPSCNotesTheme
import com.example.bpscnotes.presentation.navigation.NavGraph.BpscNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        enableEdgeToEdge()

        setContent {
            BPSCNotesTheme {
                val navController = rememberNavController()
                BpscNavHost(navController = navController)
            }
        }
    }
}