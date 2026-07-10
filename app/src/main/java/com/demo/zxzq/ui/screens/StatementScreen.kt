package com.demo.zxzq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.zxzq.data.MockData
import com.demo.zxzq.data.StatementItem
import com.demo.zxzq.ui.theme.ZxColors

/** 交割单页（图3）。 */
@Composable
fun StatementScreen(onBack: () -> Unit = {}) {
    var tab by remember { mutableIntStateOf(2) } // 默认 自定义

    Column(Modifier.fillMaxSize().background(ZxColors.ScreenBg)) {
        // 顶栏
        Box(
            Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "返回",
                tint = ZxColors.TextPrimary,
                modifier = Modifier.align(Alignment.CenterStart).size(28.dp).clickable { onBack() }
            )
            Text(
                "交割单", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = ZxColors.TextPrimary, modifier = Modifier.align(Alignment.Center)
            )
        }

        // 近一周 / 近一月 / 自定义 + 搜索
        Row(
            Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("近一周", "近一月", "自定义").forEachIndexed { i, t ->
                val sel = i == tab
                Column(
                    Modifier.weight(1f).clickable { tab = i },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        t, fontSize = 15.sp,
                        color = ZxColors.TextPrimary,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.width(22.dp).height(3.dp).background(
                            if (sel) ZxColors.TabIndicator else Color.Transparent, RoundedCornerShape(2.dp)
                        )
                    )
                }
            }
            Icon(Icons.Outlined.Search, "搜索", tint = ZxColors.TextPrimary, modifier = Modifier.size(24.dp).padding(end = 4.dp))
        }

        // 日期区间（仅自定义显示）
        if (tab == 2) {
            Row(
                Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateChip("2025-07-02", Modifier.weight(1f))
                Text("至", fontSize = 14.sp, color = ZxColors.TextSecondary, modifier = Modifier.padding(horizontal = 12.dp))
                DateChip("2026-07-01", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(MockData.statements) { StatementCard(it) }
        }
    }
}

@Composable
private fun DateChip(text: String, modifier: Modifier) {
    Box(
        modifier
            .background(ZxColors.ChipBg, RoundedCornerShape(6.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 15.sp, color = ZxColors.TextPrimary)
    }
}

@Composable
private fun StatementCard(s: StatementItem) {
    // 发生金额为负=绿（支出），正=红/黑（收入），这里按 A 股：正红负绿
    val amountColor = if (s.amount.startsWith("-")) ZxColors.Down else ZxColors.TextPrimary
    Column(Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(16.dp)) {
        // 标题行：竖标"已交收" + 标题/日期 ... 发生金额/资金余额
        Row {
            Box(
                Modifier.background(Color(0xFFEAF2FE), RoundedCornerShape(3.dp)).padding(horizontal = 2.dp, vertical = 3.dp)
            ) {
                Text("已\n交\n收", fontSize = 9.sp, color = ZxColors.LinkBlue, lineHeight = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(s.date, fontSize = 12.sp, color = ZxColors.TextTertiary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row {
                    Text("发生金额 ", fontSize = 12.sp, color = ZxColors.TextSecondary)
                    Text(s.amount, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = amountColor)
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    Text("资金余额 ", fontSize = 12.sp, color = ZxColors.TextSecondary)
                    Text(s.balance, fontSize = 14.sp, color = ZxColors.TextPrimary)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 费用明细：两列三行
        DetailRow("成交金额", s.turnover, "手续费", s.fee)
        DetailRow("印花税", s.stampTax, "过户费", s.transferFee)
        DetailRow("附加费", s.surcharge, "交易所清算费", s.clearingFee)

        Spacer(Modifier.height(12.dp))

        // 证券信息浅底块
        Column(
            Modifier.fillMaxWidth().background(ZxColors.ChipBg, RoundedCornerShape(8.dp)).padding(14.dp)
        ) {
            DetailRow("证券名称", s.secName, "证券代码", s.secCode, boldValue = true)
            Spacer(Modifier.height(10.dp))
            DetailRow("成交价格", s.dealPrice, "成交数量", s.dealQty, boldValue = true)
            Spacer(Modifier.height(10.dp))
            Row {
                Text("证券余额", fontSize = 14.sp, color = ZxColors.TextSecondary, modifier = Modifier.width(72.dp))
                Text(s.secBalance, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).background(ZxColors.ScreenBg))
    }
}

/** 一行两组「标签 值」。 */
@Composable
private fun DetailRow(l1: String, v1: String, l2: String, v2: String, boldValue: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.weight(1f)) {
            Text(l1, fontSize = 14.sp, color = ZxColors.TextSecondary, modifier = Modifier.width(72.dp))
            Text(v1, fontSize = 14.sp, fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Normal, color = ZxColors.TextPrimary)
        }
        Row(Modifier.weight(1f)) {
            Text(l2, fontSize = 14.sp, color = ZxColors.TextSecondary, modifier = Modifier.width(88.dp))
            Text(v2, fontSize = 14.sp, fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Normal, color = ZxColors.TextPrimary)
        }
    }
}
