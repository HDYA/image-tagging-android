package com.hdya.imagetagging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.Room
import com.hdya.imagetagging.data.AppDatabase
import com.hdya.imagetagging.data.PreferencesRepository
import com.hdya.imagetagging.ui.ImageTaggingApp
import com.hdya.imagetagging.ui.theme.ImageTaggingAppTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var database: AppDatabase
    private lateinit var preferencesRepository: PreferencesRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
        
        preferencesRepository = PreferencesRepository(this)
        
        enableEdgeToEdge()
        setContent {
            ImageTaggingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImageTaggingApp(
                        database = database,
                        preferencesRepository = preferencesRepository
                    )
                }
            }
        }
    }
}