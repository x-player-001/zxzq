package com.demo.zxzq.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.zxzq.data.MockData
import com.demo.zxzq.data.Quote
import com.demo.zxzq.data.QuoteRepository
import com.demo.zxzq.data.money
import com.demo.zxzq.ui.icons.ZxIcons
import com.demo.zxzq.ui.theme.ZxColors
import kotlinx.coroutines.delay

/** 交易主页（图1）。点右上“查”进入查询页。未登录时资产隐藏为 ****。 */
@Composable
fun TradeHomeScreen(isLoggedIn: Boolean = true, onOpenQuery: () -> Unit = {}) {
    // 实时行情驱动资产卡（登录后才拉）。
    var quotes by remember { mutableStateOf<Map<String, Quote>>(emptyMap()) }
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) return@LaunchedEffect
        while (true) {
            val q = QuoteRepository.fetch(MockData.holdings.map { it.code })
            if (q.isNotEmpty()) quotes = q
            delay(60_000)
        }
    }
    val s = MockData.summarize(QuoteRepository.toViews(MockData.holdings, quotes))

    LazyColumn(
        Modifier.fillMaxSize().background(ZxColors.ScreenBg),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { TopCategoryBar() }
        item { BigButtons(onQuery = onOpenQuery) }
        item { FunctionRow() }
        item { Spacer(Modifier.height(8.dp)) }
        item { AssetCard(s, hidden = !isLoggedIn) }
        item { MonthReturnCard() }
        item { BusinessGrid() }
    }
}

private fun pnl(v: Double) = if (v >= 0) ZxColors.Up else ZxColors.Down

/* ---------- 顶部分类 + 资金账号 ---------- */
@Composable
private fun TopCategoryBar() {
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("普通" to true, "信用" to false, "期权" to false, "期货" to false).forEach { (t, sel) ->
            Text(
                t, fontSize = if (sel) 20.sp else 16.sp,
                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                color = if (sel) ZxColors.TextPrimary else ZxColors.TextSecondary,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text("资金账号", fontSize = 11.sp, color = ZxColors.TextTertiary)
            Spacer(Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(MockData.MINE_TRADE_ACCOUNT, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/* ---------- 买/卖/撤/持/查 五大按钮 ---------- */
@Composable
private fun BigButtons(onQuery: () -> Unit) {
    data class Btn(val big: String, val sub: String, val color: Color, val onClick: (() -> Unit)? = null)
    val btns = listOf(
        Btn("买", "买入", ZxColors.Down.let { Color(0xFFE4393C) }),
        Btn("卖", "卖出", Color(0xFF2E6DE6)),
        Btn("撤", "撤单", Color(0xFFE8842A)),
        Btn("持", "持仓", Color(0xFFB98A3A)),
        Btn("查", "查询", Color(0xFF333333), onQuery),
    )
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        btns.forEach { b ->
            Column(
                Modifier.weight(1f)
                    .background(Color(0xFFF7F8FA), RoundedCornerShape(8.dp))
                    .clickable(enabled = b.onClick != null) { b.onClick?.invoke() }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(b.big, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = b.color)
                Spacer(Modifier.height(3.dp))
                Text(b.sub, fontSize = 12.sp, color = ZxColors.TextSecondary)
            }
        }
    }
}

/* ---------- 预约打新 / 买卖条件单 / 网格交易 / 智能盯盘 / 更多 ---------- */
@Composable
private fun FunctionRow() {
    data class Fn(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)
    val fns = listOf(
        Fn(ZxIcons.Alarm, "预约打新"),
        Fn(ZxIcons.Survey, "买卖条件单"),
        Fn(ZxIcons.Simulate, "网格交易"),
        Fn(ZxIcons.Alarm, "智能盯盘"),
        Fn(ZxIcons.AccountPerson, "更多"),
    )
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
    ) {
        fns.forEach { f ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(f.icon, f.label, tint = ZxColors.TextPrimary, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(8.dp))
                Text(f.label, fontSize = 12.sp, color = ZxColors.TextPrimary, maxLines = 1, softWrap = false)
            }
        }
    }
}

/* ---------- 实时资产卡 ---------- */
@Composable
private fun AssetCard(s: com.demo.zxzq.data.AccountSummary, hidden: Boolean) {
    // 未登录时资产遮罩为 ****；盈亏颜色回落中性，避免泄露红绿。
    fun mask(v: Double) = if (hidden) "****" else money(v)
    val neutral = ZxColors.TextPrimary
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            .background(ZxColors.CardBg, RoundedCornerShape(12.dp)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("人民币账户资产", fontSize = 15.sp, color = ZxColors.TextSecondary)
            Spacer(Modifier.width(6.dp))
            Icon(ZxIcons.Eye, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.weight(1f))
            Text(if (hidden) "未登录" else "刚刚更新", fontSize = 12.sp, color = ZxColors.TextTertiary)
            Icon(Icons.Outlined.History, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(mask(s.totalAsset), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary, maxLines = 1, softWrap = false)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(14.dp))
        // 总市值 / 浮动盈亏 / 当日盈亏
        Row(Modifier.fillMaxWidth()) {
            AssetMetric("总市值", mask(s.marketValue), neutral, Modifier.weight(1f), arrow = true)
            AssetMetric("浮动盈亏", mask(s.floatProfit), if (hidden) neutral else pnl(s.floatProfit), Modifier.weight(1f), help = true)
            AssetMetric("当日参考盈亏", mask(s.todayProfit), if (hidden) neutral else pnl(s.todayProfit), Modifier.weight(1.2f), help = true, suffix = if (hidden) null else String.format("%+.2f%%", s.todayPct))
        }
        Spacer(Modifier.height(16.dp))
        // 可用 / 可取 / 银证转账
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            AssetMetric("可用", mask(s.cash), neutral, Modifier.weight(1f), help = true)
            AssetMetric("可取", mask(s.cash), neutral, Modifier.weight(1f))
            Box(
                Modifier.weight(1.2f).background(ZxColors.Brand, RoundedCornerShape(50)).padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("银证转账", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AssetMetric(
    label: String, value: String, valueColor: Color, modifier: Modifier,
    arrow: Boolean = false, help: Boolean = false, suffix: String? = null
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, color = ZxColors.TextSecondary, maxLines = 1, softWrap = false)
            if (help) Icon(Icons.AutoMirrored.Outlined.HelpOutline, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, softWrap = false)
            if (suffix != null) {
                Spacer(Modifier.width(3.dp))
                Text(suffix, fontSize = 11.sp, color = valueColor, maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 2.dp))
            }
            if (arrow) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(14.dp))
        }
    }
}

/* ---------- 6月收益卡（双折线） ---------- */
@Composable
private fun MonthReturnCard() {
    Column(
        Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp).fillMaxWidth()
            .background(ZxColors.CardBg, RoundedCornerShape(12.dp)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("6月收益", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
            Spacer(Modifier.width(6.dp))
            Text("截至6月30日", fontSize = 12.sp, color = ZxColors.TextTertiary)
            Spacer(Modifier.width(4.dp))
            Icon(ZxIcons.Eye, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(MockData.MINE_MONTH_PROFIT.replace(",", ""), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ZxColors.Down)
            Text("元", fontSize = 13.sp, color = ZxColors.TextSecondary, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
            Spacer(Modifier.width(12.dp))
            Text("再加把劲！相对沪深300 ", fontSize = 13.sp, color = Color(0xFFB98A3A), modifier = Modifier.padding(bottom = 3.dp))
            Text("-4.12%", fontSize = 13.sp, color = ZxColors.Down, modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(Color(0xFFE8842A)); Text(" 月收益率  ", fontSize = 12.sp, color = ZxColors.TextSecondary)
            LegendDot(Color(0xFF2E6DE6)); Text(" 沪深300", fontSize = 12.sp, color = ZxColors.TextSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DoubleLineChart(Modifier.weight(1f).height(60.dp))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                Text("+1.78%", fontSize = 14.sp, color = Color(0xFF2E6DE6), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(14.dp))
                Text("-2.34%", fontSize = 14.sp, color = Color(0xFFE8842A), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun LegendDot(c: Color) {
    Box(Modifier.size(width = 10.dp, height = 3.dp).background(c, RoundedCornerShape(2.dp)))
}

@Composable
private fun DoubleLineChart(modifier: Modifier) {
    val orange = listOf(0.45f, 0.5f, 0.62f, 0.7f, 0.66f, 0.72f, 0.68f, 0.58f, 0.5f, 0.44f, 0.4f, 0.38f)
    val blue = listOf(0.4f, 0.38f, 0.42f, 0.5f, 0.46f, 0.55f, 0.6f, 0.52f, 0.62f, 0.58f, 0.64f, 0.6f)
    Canvas(modifier) {
        fun draw(points: List<Float>, color: Color) {
            val step = size.width / (points.size - 1)
            val path = Path()
            points.forEachIndexed { i, v ->
                val x = step * i; val y = size.height - v * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = 3f))
        }
        draw(orange, Color(0xFFE8842A))
        draw(blue, Color(0xFF2E6DE6))
    }
}

/* ---------- 业务卡片网格 ---------- */
@Composable
private fun BusinessGrid() {
    Column(Modifier.padding(top = 6.dp)) {
        BusinessRow(
            { IpoCard(Modifier.weight(1f)) },
            { RepoCard(Modifier.weight(1f)) },
        )
        BusinessRow(
            { InfoCard("港股通", "交易、投票、公司行为", Modifier.weight(1f)) },
            { InfoCard("场内基金", "REITs、ETF、LOF等", Modifier.weight(1f)) },
        )
        BusinessRow(
            { InfoCard("债券", "", Modifier.weight(1f)) },
            { InfoCard("模拟交易", "", Modifier.weight(1f)) },
        )
    }
}

@Composable
private fun BusinessRow(a: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit, b: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        a(); b()
    }
}

@Composable
private fun IpoCard(modifier: Modifier) {
    Column(modifier.background(ZxColors.CardBg, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Text("今日打新", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        Row {
            Text("新股 ", fontSize = 14.sp, color = ZxColors.TextSecondary)
            Text("1", fontSize = 14.sp, color = Color(0xFFE8842A))
            Spacer(Modifier.width(20.dp))
            Text("新债 ", fontSize = 14.sp, color = ZxColors.TextSecondary)
            Text("0", fontSize = 14.sp, color = Color(0xFFE8842A))
        }
    }
}

@Composable
private fun RepoCard(modifier: Modifier) {
    Column(modifier.background(ZxColors.CardBg, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Text("通用回购逆回购", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(Color(0xFFFFF3E6), RoundedCornerShape(3.dp)).padding(horizontal = 3.dp, vertical = 1.dp)) {
                Text("1天", fontSize = 10.sp, color = Color(0xFFE8842A))
            }
            Spacer(Modifier.width(4.dp))
            Text("沪 ", fontSize = 13.sp, color = ZxColors.TextSecondary)
            Text("1.515% ", fontSize = 13.sp, color = ZxColors.TextPrimary)
            Text("深 ", fontSize = 13.sp, color = ZxColors.TextSecondary)
            Text("1.490%", fontSize = 13.sp, color = ZxColors.TextPrimary)
        }
    }
}

@Composable
private fun InfoCard(title: String, sub: String, modifier: Modifier) {
    Column(modifier.background(ZxColors.CardBg, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
        if (sub.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(sub, fontSize = 13.sp, color = ZxColors.TextSecondary)
        } else {
            Spacer(Modifier.height(10.dp))
            Text(" ", fontSize = 13.sp)
        }
    }
}
