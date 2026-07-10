package com.demo.zxzq.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.demo.zxzq.R

/**
 * HarmonyOS Sans（鸿蒙字体，华为设备默认字形）。
 *
 * 仅内置拉丁字重（含数字，约 146KB×3）：数字/英文用鸿蒙字形，
 * 中文因该字体无中文字形会自动回落系统字体 —— 效果即“数字鸿蒙、中文系统”。
 *
 * 字体文件：app/src/main/res/font/harmonyos_sans_{regular,medium,bold}.ttf
 *
 * 全局生效：通过 ZxTypography 配进 MaterialTheme，所有 Text 默认继承，
 * 无需在每个 Text 上手写 fontFamily。改字体只改这里。
 */
val HarmonyFont = FontFamily(
    Font(R.font.harmonyos_sans_regular, FontWeight.Normal),
    Font(R.font.harmonyos_sans_medium, FontWeight.Medium),
    Font(R.font.harmonyos_sans_bold, FontWeight.Bold),
)

/** 把默认字体设为鸿蒙的 Typography（在 M3 默认排版基础上统一替换 fontFamily）。 */
val ZxTypography: Typography = Typography().let { d ->
    Typography(
        displayLarge = d.displayLarge.copy(fontFamily = HarmonyFont),
        displayMedium = d.displayMedium.copy(fontFamily = HarmonyFont),
        displaySmall = d.displaySmall.copy(fontFamily = HarmonyFont),
        headlineLarge = d.headlineLarge.copy(fontFamily = HarmonyFont),
        headlineMedium = d.headlineMedium.copy(fontFamily = HarmonyFont),
        headlineSmall = d.headlineSmall.copy(fontFamily = HarmonyFont),
        titleLarge = d.titleLarge.copy(fontFamily = HarmonyFont),
        titleMedium = d.titleMedium.copy(fontFamily = HarmonyFont),
        titleSmall = d.titleSmall.copy(fontFamily = HarmonyFont),
        bodyLarge = d.bodyLarge.copy(fontFamily = HarmonyFont),
        bodyMedium = d.bodyMedium.copy(fontFamily = HarmonyFont),
        bodySmall = d.bodySmall.copy(fontFamily = HarmonyFont),
        labelLarge = d.labelLarge.copy(fontFamily = HarmonyFont),
        labelMedium = d.labelMedium.copy(fontFamily = HarmonyFont),
        labelSmall = d.labelSmall.copy(fontFamily = HarmonyFont),
    )
}
