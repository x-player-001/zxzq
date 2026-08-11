package com.demo.zxzq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.zxzq.data.MockData
import com.demo.zxzq.ui.icons.ZxIcons
import com.demo.zxzq.ui.theme.ZxColors

/** 委托交易·查询页（图2）。点“交割单”进入交割单页。 */
@Composable
fun TradeQueryScreen(onBack: () -> Unit = {}, onOpenStatement: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(ZxColors.ScreenBg)) {
        // 顶栏
        Box(Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "返回",
                tint = ZxColors.TextPrimary,
                modifier = Modifier.align(Alignment.CenterStart).size(28.dp).clickable { onBack() }
            )
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("委托交易", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text("${MockData.TRADE_NAME}  客户号:${MockData.TRADE_CLIENT_NO}", fontSize = 12.sp, color = ZxColors.TextSecondary)
            }
            Icon(ZxIcons.Settings, "设置", tint = ZxColors.TextPrimary, modifier = Modifier.align(Alignment.CenterEnd).size(24.dp))
        }

        // 买入/卖出/撤单/持仓/查询（查询选中）
        Row(Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(vertical = 12.dp)) {
            listOf("买入", "卖出", "撤单", "持仓", "查询").forEachIndexed { i, t ->
                val sel = i == 4
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t, fontSize = 16.sp, color = ZxColors.TextPrimary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(20.dp).height(3.dp).background(if (sel) ZxColors.TabIndicator else Color.Transparent, RoundedCornerShape(2.dp)))
                }
            }
        }

        TradeQueryContent(onOpenStatement = onOpenStatement)
    }
}

/** 查询宫格内容（无脚手架）。查询页 overlay 与交易页「查询」tab 共用。 */
@Composable
fun TradeQueryContent(onOpenStatement: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        QuerySection("委托与成交", listOf(
            "当日委托", "当日成交", "当日成交汇总",
            "历史委托", "历史成交", "历史成交汇总",
            "在途证券", "已清仓证券", "T操作",
        ))
        QuerySection("流水查询", listOf(
            "交割单", "对账单", "证券退市流水",
            "当日资金流水", "历史资金流水", "转账资金汇总",
        ), onItemClick = { label -> if (label == "交割单") onOpenStatement() })
        QuerySection("更多查询", listOf(
            "普通账号\n及交易权限", "资金查询", "限售股查询",
            "其他查询",
        ))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuerySection(title: String, items: List<String>, onItemClick: (String) -> Unit = {}) {
    Column(Modifier.fillMaxWidth().background(ZxColors.ScreenBg).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(title, fontSize = 15.sp, color = ZxColors.TextSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        // 每行 3 个
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { label ->
                    QueryCell(label, Modifier.weight(1f), onClick = { onItemClick(label) })
                }
                // 补齐空位保持等宽
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun QueryCell(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(56.dp)
            .background(ZxColors.ChipBg, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontSize = 15.sp, color = ZxColors.TextPrimary,
            textAlign = TextAlign.Center, lineHeight = 18.sp, fontWeight = FontWeight.Medium
        )
    }
}
