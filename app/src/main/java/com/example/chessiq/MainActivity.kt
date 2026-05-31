package com.example.chessiq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.chessiq.ui.ChessGameScreen
import com.example.chessiq.ui.theme.ChessiqTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessiqTheme {
                ChessGameScreen()
            }
        }
    }
}