package com.demo.zxzq.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.demo.zxzq.ui.theme.ZxColors

/**
 * 直接把整张参考图作为页面显示：宽度铺满，按原图比例纵向缩放，超出高度可滚动。
 */
@Composable
fun ImageScreen(@DrawableRes imageRes: Int) {
    Image(
        painter = painterResource(imageRes),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxSize()
            .background(ZxColors.ScreenBg)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
    )
}
