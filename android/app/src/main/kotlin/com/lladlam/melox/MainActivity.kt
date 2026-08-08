package com.lladlam.melox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lladlam.melox.ui.MeloXApp
import com.lladlam.melox.ui.theme.MeloXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeloXTheme {
                MeloXApp()
            }
        }
    }
}
