package com.demo.zxzq.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.demo.zxzq.R

/**
 * 数字/英文专用字体 —— Harmonia Sans Semi Bold Condensed（窄体，数字 4 闭口、偏瘦）。
 *
 * **只给显示数字/英文的 Text 手动指定 `fontFamily = NumberFont`**，
 * 不走全局 Typography——因为全局会波及汉字的字重（汉字回落系统字体时被误加粗）。
 * 这样汉字完全保持系统默认，不受影响。
 *
 * 字体文件：app/src/main/res/font/harmonia_sans.ttf
 */
val NumberFont = FontFamily(Font(R.font.harmonia_sans))
