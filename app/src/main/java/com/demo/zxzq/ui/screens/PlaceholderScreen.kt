package com.demo.zxzq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.demo.zxzq.ui.theme.ZxColors

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        Modifier.fillMaxSize().background(ZxColors.ScreenBg),
        contentAlignment = Alignment.Center
    ) {
        Text("「$title」页 · 待实现", color = ZxColors.TextSecondary, fontSize = 15.sp)
    }
}
