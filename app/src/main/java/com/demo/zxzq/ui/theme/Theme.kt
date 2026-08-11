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
    // 不设全局 typography：汉字保持系统默认字体（避免被自定义字体的字重误加粗）。
    // 数字/英文单独在各 Text 上用 fontFamily = NumberFont（见 ZxFonts.kt）。
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
