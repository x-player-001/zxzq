package com.demo.zxzq.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.zxzq.data.HoldingView
import com.demo.zxzq.data.MockData
import com.demo.zxzq.data.Quote
import com.demo.zxzq.data.QuoteRepository
import com.demo.zxzq.data.money
import com.demo.zxzq.ui.icons.ZxIcons
import com.demo.zxzq.ui.theme.ZxColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TradeScreen(onBack: () -> Unit = {}) {
    var tab by remember { mutableIntStateOf(3) } // 默认 持仓

    // 实时行情：每 1 分钟拉一次新浪行情。
    var quotes by remember { mutableStateOf<Map<String, Quote>>(emptyMap()) }
    var refreshing by remember { mutableStateOf(false) }
    val codes = remember { MockData.holdings.map { it.code } }

    suspend fun refresh() {
        refreshing = true
        val q = QuoteRepository.fetch(codes)
        if (q.isNotEmpty()) quotes = q
        refreshing = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(60_000)
        }
    }

    // 合成持仓视图（现价缺失时用成本价兜底）。
    val views = QuoteRepository.toViews(MockData.holdings, quotes)
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(ZxColors.ScreenBg)) {
        TradeTopBar(onBack = onBack)
        TradeTabs(selected = tab, onSelect = { tab = it })
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(ZxColors.Divider))
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { AssetSummary(views) }
            item { Spacer(Modifier.height(8.dp).fillMaxWidth().background(ZxColors.ScreenBg)) }
            item { HoldingHeader() }
            item { CategoryChips() }
            item { ColumnTitles() }
            item { AccountLine() }
            items(views) { HoldingRow(it) }
            item { FooterLinks(refreshing = refreshing, onRefresh = { scope.launch { refresh() } }) }
        }
    }
}

private fun pct(v: Double): String = String.format("%+.2f%%", v)

/* ----------------------------- 顶部标题栏 ----------------------------- */

@Composable
private fun TradeTopBar(onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(ZxColors.CardBg)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "返回",
            tint = ZxColors.TextPrimary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(28.dp)
                .clickable { onBack() }
        )
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("委托交易", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                "${MockData.TRADE_NAME}  客户号:${MockData.TRADE_CLIENT_NO}",
                fontSize = 12.sp, color = ZxColors.TextSecondary
            )
        }
        Icon(
            ZxIcons.Settings, "设置",
            tint = ZxColors.TextPrimary,
            modifier = Modifier.align(Alignment.CenterEnd).size(24.dp)
        )
    }
}

/* ----------------------------- 买入/卖出/撤单/持仓/查询 ----------------------------- */

@Composable
private fun TradeTabs(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("买入", "卖出", "撤单", "持仓", "查询")
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(vertical = 12.dp)
    ) {
        tabs.forEachIndexed { i, t ->
            val isSel = i == selected
            Column(
                Modifier.weight(1f).clickable { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    t,
                    fontSize = 16.sp,
                    color = ZxColors.TextPrimary,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .width(20.dp)
                        .height(3.dp)
                        .background(
                            if (isSel) ZxColors.TabIndicator else Color.Transparent,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

/* ----------------------------- 资产数据区 ----------------------------- */

@Composable
private fun AssetSummary(views: List<HoldingView>) {
    // 实时汇总：总市值 / 浮动盈亏 / 当日参考盈亏 / 总资产（与我的页共用同一套计算）。
    val s = MockData.summarize(views)
    val available = s.cash
    val marketValue = s.marketValue
    val floatProfit = s.floatProfit
    val todayProfit = s.todayProfit
    val totalAsset = s.totalAsset
    val todayPct = s.todayPct

    Column(Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(16.dp)) {
        // 人民币账户 A股 + 仓位
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 22.dp, height = 15.dp)
                    .background(Color(0xFFDE2910), RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("★", color = Color(0xFFFFDE00), fontSize = 9.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text("人民币账户 A股", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.background(ZxColors.ChipBg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(String.format("仓位%.2f%%", s.positionRate), fontSize = 13.sp, color = ZxColors.TextSecondary)
            }
        }

        Spacer(Modifier.height(18.dp))

        // 第一行：资产 / 浮动盈亏 / 当日参考盈亏
        Row(Modifier.fillMaxWidth()) {
            MetricCol(
                modifier = Modifier.weight(1f),
                label = "资产", labelIcon = MetricIcon.Eye,
                value = money(totalAsset), valueColor = ZxColors.TextPrimary, arrow = true
            )
            MetricCol(
                modifier = Modifier.weight(1f),
                label = "浮动盈亏", labelIcon = MetricIcon.Help,
                value = money(floatProfit), valueColor = pnlColor(floatProfit)
            )
            MetricCol(
                modifier = Modifier.weight(1.2f),
                label = "当日参考盈亏", labelIcon = MetricIcon.Help,
                value = money(todayProfit), valueColor = pnlColor(todayProfit),
                suffix = pct(todayPct), arrow = true
            )
        }

        Spacer(Modifier.height(18.dp))

        // 第二行：总市值 / 可用(逆回购) / 可取(转账)
        Row(Modifier.fillMaxWidth()) {
            MetricCol(
                modifier = Modifier.weight(1f),
                label = "总市值", value = money(marketValue), valueColor = ZxColors.TextPrimary
            )
            Column(Modifier.weight(1f)) {
                LabelWithTag("可用", "逆回购")
                Spacer(Modifier.height(6.dp))
                Text(money(available), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary, maxLines = 1, softWrap = false)
            }
            Column(Modifier.weight(1f)) {
                LabelWithTag("可取", "转账")
                Spacer(Modifier.height(6.dp))
                Text(money(available), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** 盈亏颜色：正红负绿（A 股习惯）。 */
private fun pnlColor(v: Double) = if (v >= 0) ZxColors.Up else ZxColors.Down

private enum class MetricIcon { None, Eye, Help }

@Composable
private fun MetricCol(
    modifier: Modifier = Modifier,
    label: String,
    labelIcon: MetricIcon = MetricIcon.None,
    value: String,
    valueColor: Color,
    suffix: String? = null,
    arrow: Boolean = false
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, color = ZxColors.TextSecondary, maxLines = 1, softWrap = false)
            Spacer(Modifier.width(3.dp))
            when (labelIcon) {
                MetricIcon.Eye -> Icon(ZxIcons.Eye, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(12.dp))
                MetricIcon.Help -> Icon(Icons.AutoMirrored.Outlined.HelpOutline, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(12.dp))
                MetricIcon.None -> {}
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, softWrap = false)
            if (suffix != null) {
                Spacer(Modifier.width(3.dp))
                Text(suffix, fontSize = 11.sp, color = valueColor, maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 2.dp))
            }
            if (arrow) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(14.dp).padding(bottom = 1.dp))
            }
        }
    }
}

@Composable
private fun LabelWithTag(label: String, tag: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = ZxColors.TextSecondary)
        Icon(Icons.AutoMirrored.Outlined.HelpOutline, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.background(ZxColors.ChipBgRedLight, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Text(tag, fontSize = 11.sp, color = ZxColors.Brand)
        }
    }
}

/* ----------------------------- 持仓证券头 ----------------------------- */

@Composable
private fun HoldingHeader() {
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("持仓证券", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
        Spacer(Modifier.weight(1f))
        Icon(ZxIcons.Chart, null, tint = ZxColors.TextSecondary, modifier = Modifier.size(16.dp))
        Text(" 行情", fontSize = 14.sp, color = ZxColors.TextSecondary)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(0.5.dp).height(14.dp).background(ZxColors.Divider))
        Spacer(Modifier.width(12.dp))
        Icon(ZxIcons.Chat, null, tint = ZxColors.TextSecondary, modifier = Modifier.size(16.dp))
        Text(" 资讯", fontSize = 14.sp, color = ZxColors.TextSecondary)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CategoryChips() {
    val chips = listOf("全部(3)" to true, "股票(3)" to false, "债券(0)" to false, "基金(0)" to false, "其他(0)" to false)
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chips.forEach { (text, sel) ->
            Box(
                Modifier
                    .weight(1f)
                    .background(if (sel) ZxColors.ChipBgRedLight else ZxColors.ChipBg, RoundedCornerShape(6.dp))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text, fontSize = 12.sp,
                    color = if (sel) ZxColors.Brand else ZxColors.TextSecondary,
                    fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1, softWrap = false
                )
            }
        }
    }
}

@Composable
private fun ColumnTitles() {
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text("市值", fontSize = 13.sp, color = ZxColors.Brand, fontWeight = FontWeight.Medium)
            Text(" ↓", fontSize = 12.sp, color = ZxColors.Brand)
        }
        Text("盈亏", fontSize = 13.sp, color = ZxColors.TextSecondary, modifier = Modifier.weight(1f))
        Text("持仓/ 可用", fontSize = 13.sp, color = ZxColors.TextSecondary, modifier = Modifier.weight(1f))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("成本/ 市价", fontSize = 13.sp, color = ZxColors.TextSecondary)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun AccountLine() {
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("人民币账户 A 股", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = ZxColors.TextPrimary)
        Spacer(Modifier.weight(1f))
        Text("持有 3 只", fontSize = 14.sp, color = ZxColors.TextSecondary)
        Icon(Icons.Outlined.KeyboardArrowUp, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(18.dp))
    }
}

/* ----------------------------- 持仓行 ----------------------------- */

@Composable
private fun HoldingRow(v: HoldingView) {
    val color = if (v.isUp) ZxColors.Up else ZxColors.Down
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        // 名称 + 市值
        Column(Modifier.weight(1f)) {
            Text(v.holding.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ZxColors.TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(money(v.marketValue), fontSize = 13.sp, color = ZxColors.TextPrimary)
        }
        // 盈亏 + 比例
        Column(Modifier.weight(1f)) {
            Text(money(v.profit), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
            Spacer(Modifier.height(6.dp))
            Text(pct(v.profitPct), fontSize = 13.sp, color = color)
        }
        // 持仓 / 可用
        Column(Modifier.weight(1f)) {
            Text(v.holding.shares.toString(), fontSize = 14.sp, color = ZxColors.TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(v.holding.shares.toString(), fontSize = 13.sp, color = ZxColors.TextSecondary)
        }
        // 成本 / 市价
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(String.format("%.4f", v.holding.cost), fontSize = 14.sp, color = ZxColors.TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(String.format("%.4f", v.price), fontSize = 13.sp, color = color)
        }
    }
}

/* ----------------------------- 在途/已清仓 + 刷新 ----------------------------- */

@Composable
private fun FooterLinks(refreshing: Boolean, onRefresh: () -> Unit) {
    // 刷新中时图标持续旋转。
    val angle by animateFloatAsState(
        targetValue = if (refreshing) 360f else 0f,
        animationSpec = if (refreshing)
            infiniteRepeatable(tween(800, easing = LinearEasing))
        else tween(0),
        label = "refresh"
    )
    Box {
        Column(Modifier.background(ZxColors.ScreenBg)) {
            Spacer(Modifier.height(8.dp))
            FooterRow("在途证券")
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(ZxColors.Divider))
            FooterRow("已清仓证券")
            Spacer(Modifier.height(40.dp))
        }
        // 刷新悬浮按钮
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 8.dp)
                .size(48.dp)
                .background(Color(0xFF8A8F99), RoundedCornerShape(10.dp))
                .clickable { onRefresh() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                ZxIcons.Refresh, "刷新", tint = Color.White,
                modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = angle }
            )
        }
    }
}

@Composable
private fun FooterRow(text: String) {
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 16.sp, color = ZxColors.TextPrimary)
        Spacer(Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(20.dp))
    }
}
