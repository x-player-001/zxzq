package com.demo.zxzq.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 参考中信证券 App 的配色。主色为品牌红，涨红跌绿（A 股习惯）。
 */
object ZxColors {
    val Brand = Color(0xFFE60012)          // 品牌红 / 强调色
    val BrandDark = Color(0xFFD5000F)

    val Up = Color(0xFFE93030)             // 涨（红）
    val Down = Color(0xFF00A05A)           // 跌（绿）
    val Flat = Color(0xFF9AA0A6)           // 平

    val TextPrimary = Color(0xFF1A1A1A)    // 主要文字
    val TextSecondary = Color(0xFF9096A0)  // 次要 / 标签文字
    val TextTertiary = Color(0xFFB4B9C2)

    val Divider = Color(0xFFEEF0F2)
    val CardBg = Color(0xFFFFFFFF)
    val ScreenBg = Color(0xFFF4F5F7)       // 页面浅灰底
    val ChipBg = Color(0xFFF2F3F5)
    val ChipBgRedLight = Color(0xFFFDECEC) // 逆回购/转账等浅红标签底
    val TabIndicator = Color(0xFF1A1A1A)

    val LinkBlue = Color(0xFF2F7DFF)
}
