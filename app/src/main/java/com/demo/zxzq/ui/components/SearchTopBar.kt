package com.demo.zxzq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import com.demo.zxzq.ui.icons.ZxIcons
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.zxzq.ui.theme.ZxColors

/** 首页 / 我的页顶部的搜索栏 + 消息/客服/设置图标（对应参考图顶部）。 */
@Composable
fun SearchTopBar() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(ZxColors.CardBg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 搜索框
        Row(
            Modifier
                .weight(1f)
                .height(38.dp)
                .background(ZxColors.ChipBg, RoundedCornerShape(50))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(ZxIcons.Search, null, tint = ZxColors.TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("7×24小时手机开户", fontSize = 14.sp, color = ZxColors.TextSecondary, modifier = Modifier.weight(1f))
            Box(Modifier.width(0.5.dp).height(16.dp).background(ZxColors.Divider))
            Spacer(Modifier.width(12.dp))
            Text("搜索", fontSize = 14.sp, color = ZxColors.Brand, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(14.dp))
        // 消息（带红点角标）
        Box {
            Icon(ZxIcons.Mail, "消息", tint = ZxColors.TextPrimary, modifier = Modifier.size(24.dp))
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .background(ZxColors.Brand, CircleShape)
                    .padding(horizontal = 4.dp)
            ) {
                Text("40", color = Color.White, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.width(16.dp))
        Icon(ZxIcons.Headset, "客服", tint = ZxColors.TextPrimary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Icon(ZxIcons.Settings, "设置", tint = ZxColors.TextPrimary, modifier = Modifier.size(24.dp))
    }
}
