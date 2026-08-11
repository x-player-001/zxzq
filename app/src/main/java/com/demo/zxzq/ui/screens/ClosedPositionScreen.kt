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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.demo.zxzq.data.ClosedPosition
import com.demo.zxzq.data.TradeRepository
import com.demo.zxzq.data.money
import com.demo.zxzq.ui.theme.ZxColors
import com.demo.zxzq.ui.theme.NumberFont

/** 已清仓证券页：展示每只清仓证券的已实现盈亏（全口径，含分红）。 */
@Composable
fun ClosedPositionScreen(onBack: () -> Unit = {}) {
    val tradeState by TradeRepository.state.collectAsState()
    // 按记录先后倒序：closed 列表尾部是最近卖出/合并的，reversed 后排最上。
    val closed = tradeState.closed.asReversed()

    Column(Modifier.fillMaxSize().background(ZxColors.ScreenBg)) {
        // 顶栏
        Box(Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 12.dp, vertical = 12.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "返回",
                tint = ZxColors.TextPrimary,
                modifier = Modifier.align(Alignment.CenterStart).size(28.dp).clickable { onBack() }
            )
            Text(
                "已清仓证券", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = ZxColors.TextPrimary, modifier = Modifier.align(Alignment.Center)
            )
        }

        if (closed.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无已清仓证券", color = ZxColors.TextSecondary, fontSize = 15.sp)
            }
        } else {
            val totalPnl = closed.sumOf { it.realizedPnl }
            val totalDividend = closed.sumOf { it.dividend }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { TotalSummary(totalPnl = totalPnl, totalDividend = totalDividend, count = closed.size) }
                items(closed) { ClosedCard(it) }
            }
        }
    }
}

/** 盈亏颜色：正红负绿（A 股习惯）。 */
private fun pnlColor(v: Double) = if (v >= 0) ZxColors.Up else ZxColors.Down

/** 顶部汇总：全部已清仓证券的总盈亏 + 总分红。 */
@Composable
private fun TotalSummary(totalPnl: Double, totalDividend: Double, count: Int) {
    Column(Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("总盈亏", fontSize = 14.sp, color = ZxColors.TextSecondary)
            Spacer(Modifier.weight(1f))
            Text(
                money(totalPnl), fontSize = 24.sp, fontFamily = NumberFont, fontWeight = FontWeight.Bold,
                color = pnlColor(totalPnl)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("共 $count 笔清仓记录", fontSize = 12.sp, color = ZxColors.TextTertiary)
            Spacer(Modifier.weight(1f))
            Text("累计分红 ", fontSize = 12.sp, color = ZxColors.TextSecondary)
            Text(money(totalDividend), fontSize = 13.sp, fontFamily = NumberFont, fontWeight = FontWeight.Medium, color = ZxColors.Brand)
        }
    }
}

@Composable
private fun ClosedCard(c: ClosedPosition) {
    val color = pnlColor(c.realizedPnl)
    Column(Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 12.dp, vertical = 8.dp)) {
        // 名称/代码 + 部分/全部清仓标签 + 已实现盈亏
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .background(ZxColors.ScreenBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            if (c.partial) "部分清仓" else "全部清仓", fontSize = 10.sp,
                            color = if (c.partial) ZxColors.Brand else ZxColors.TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${c.code.dropWhile { !it.isDigit() }}  ·  卖出 ${c.soldShares} 股",
                    fontSize = 11.sp, color = ZxColors.TextTertiary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(c.realizedPnl), fontSize = 17.sp, fontFamily = NumberFont, fontWeight = FontWeight.Bold, color = color)
                Text(String.format("已实现 %+.2f%%", c.pnlPct), fontSize = 11.sp, fontFamily = NumberFont, color = color)
            }
        }

        Spacer(Modifier.height(6.dp))

        // 买入均价 / 卖出均价
        DetailLine("买入均价", String.format("%.3f", c.buyAvg), "卖出均价", String.format("%.3f", c.sellAvg))
        Spacer(Modifier.height(4.dp))
        // 累计成本 / 累计卖出
        DetailLine("累计成本", money(c.soldCost), "累计卖出", money(c.proceeds))
        // 分红/红利税：仅全部清仓且有分红时显示（部分清仓恒为 0，省略以瘦身）
        if (c.dividend > 0.0 || c.dividendTax > 0.0) {
            Spacer(Modifier.height(4.dp))
            DetailLine(
                "分红入账", money(c.dividend),
                "红利税", if (c.dividendTax > 0) "-${money(c.dividendTax)}" else money(0.0)
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "建仓 ${c.firstBuyDate}  →  ${if (c.partial) "卖出" else "清仓"} ${c.closeDate}",
            fontSize = 10.sp, color = ZxColors.TextTertiary
        )
    }
}

@Composable
private fun DetailLine(l1: String, v1: String, l2: String, v2: String) {
    Row(Modifier.fillMaxWidth()) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(l1, fontSize = 11.sp, color = ZxColors.TextSecondary)
            Spacer(Modifier.width(6.dp))
            Text(v1, fontSize = 12.sp, fontFamily = NumberFont, color = ZxColors.TextPrimary)
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(l2, fontSize = 11.sp, color = ZxColors.TextSecondary)
            Spacer(Modifier.width(6.dp))
            Text(v2, fontSize = 12.sp, fontFamily = NumberFont, color = ZxColors.TextPrimary)
        }
    }
}
