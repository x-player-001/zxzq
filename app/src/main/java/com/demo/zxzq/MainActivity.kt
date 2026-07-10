package com.demo.zxzq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.demo.zxzq.ui.AppScaffold
import com.demo.zxzq.ui.theme.ZxColors
import com.demo.zxzq.ui.theme.ZxzqTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZxzqTheme {
                Surface(Modifier.fillMaxSize().background(ZxColors.ScreenBg)) {
                    AppScaffold()
                }
            }
        }
    }
}
