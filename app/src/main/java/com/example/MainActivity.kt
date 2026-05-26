package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.JourneyRepository
import com.example.ui.screens.JourneyMapScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.JourneyViewModel
import com.example.viewmodel.JourneyViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = JourneyRepository(database.journeyDao)
        
        // Instantiate the ViewModel
        val viewModel: JourneyViewModel by viewModels {
            JourneyViewModelFactory(repository)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    JourneyMapScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

