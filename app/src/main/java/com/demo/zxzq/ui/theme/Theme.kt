package com.demo.zxzq.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = ZxColors.Brand,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    background = ZxColors.ScreenBg,
    surface = ZxColors.CardBg,
    onBackground = ZxColors.TextPrimary,
    onSurface = ZxColors.TextPrimary,
)

@Composable
fun ZxzqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Demo 仅提供浅色主题（还原原 App 亮色界面）。
    // 全局字体 = 鸿蒙（ZxTypography），所有 Text 默认继承，无需逐个指定 fontFamily。
    MaterialTheme(
        colorScheme = LightColors,
        typography = ZxTypography,
        content = content
    )
}
