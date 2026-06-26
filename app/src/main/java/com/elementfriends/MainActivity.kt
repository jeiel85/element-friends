package com.elementfriends

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.elementfriends.ui.audio.SoundSynth
import com.elementfriends.ui.screens.LabScreen
import com.elementfriends.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    SoundSynth.initialize(applicationContext)
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          LabScreen(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    SoundSynth.startBgm()
  }

  override fun onStop() {
    super.onStop()
    SoundSynth.stopBgm()
  }
}
