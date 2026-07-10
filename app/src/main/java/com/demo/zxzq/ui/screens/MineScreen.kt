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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowRight
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
import com.demo.zxzq.data.AccountSummary
import com.demo.zxzq.data.MockData
import com.demo.zxzq.data.Quote
import com.demo.zxzq.data.QuoteRepository
import com.demo.zxzq.data.money
import com.demo.zxzq.ui.components.SearchTopBar
import com.demo.zxzq.ui.icons.ZxIcons
import com.demo.zxzq.ui.theme.ZxColors
import kotlinx.coroutines.delay

@Composable
fun MineScreen(
    isLoggedIn: Boolean = false,
    onLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenHolding: () -> Unit = {},
) {
    // 已登录时拉行情，按持仓算资产（与交易页共用同一套计算）。
    var quotes by remember { mutableStateOf<Map<String, Quote>>(emptyMap()) }
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) return@LaunchedEffect
        while (true) {
            val q = QuoteRepository.fetch(MockData.holdings.map { it.code })
            if (q.isNotEmpty()) quotes = q
            delay(60_000)
        }
    }
    val summary = MockData.summarize(QuoteRepository.toViews(MockData.holdings, quotes))

    Column(Modifier.fillMaxSize().background(ZxColors.ScreenBg)) {
        SearchTopBar()
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { AccountHeader() }
            if (isLoggedIn) {
                // 已登录：显示真实资产（图3）+ 资产分析。
                item { LoggedInAssetCard(summary = summary, onOpenHolding = onOpenHolding, onLogout = onLogout) }
                item { AnalysisRow() }
            } else {
                // 未登录：折叠资产卡，金额 ****（图1）。
                item { LoggedOutAssetCard(onLogin = onLogin) }
            }
            // 宫格区（两态都显示）。
            item { QuickTradeSection() }
            item { GridSection("业务办理", businessItems, showArrow = true) }
            item { GridSection("我的服务", serviceItems) }
            item { GridSection("功能专区", featureItems, showArrow = true) }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/* ----------------------------- 头像 / 账户区 ----------------------------- */

@Composable
private fun AccountHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 吉祥物头像占位（品牌红圆底 + 首字）。
        Box(
            Modifier
                .size(48.dp)
                .background(Color(0xFFFFF3D6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("牛", fontSize = 20.sp, color = Color(0xFFE0A93B), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                MockData.MINE_PHONE,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ZxColors.TextPrimary
            )
            Spacer(Modifier.height(5.dp))
            Row {
                TagChip("签到", Color(0xFF9A6A2B), Color(0xFFF6ECD9))
                Spacer(Modifier.width(8.dp))
                TagChip("低碳", Color(0xFF2E9BD6), Color(0xFFE4F3FB))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("个人主页", fontSize = 12.sp, color = ZxColors.TextSecondary)
            Icon(
                Icons.Outlined.KeyboardArrowRight, null,
                tint = ZxColors.TextTertiary, modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TagChip(text: String, textColor: Color, bg: Color) {
    Box(
        Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

/* ----------------------------- 资产卡片：未登录态（图1） ----------------------------- */

@Composable
private fun LoggedOutAssetCard(onLogin: () -> Unit) {
    Card {
        // “登录交易账号查看 >”
        Row(
            Modifier.fillMaxWidth().clickable { onLogin() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("登录交易账号查看", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
            Icon(Icons.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextSecondary, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(20.dp))

        // 总资产 **** / 昨日收益 ****
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("人民币总资产(元)", fontSize = 12.sp, color = ZxColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Text("****", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("昨日收益(元)", fontSize = 12.sp, color = ZxColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Text("****", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
            }
        }
    }
}

/* ----------------------------- 资产卡片：已登录态（图3） ----------------------------- */

/** 盈亏颜色：正红负绿（A 股习惯）。 */
private fun pnlColor(v: Double) = if (v >= 0) ZxColors.Up else ZxColors.Down

@Composable
private fun LoggedInAssetCard(
    summary: AccountSummary,
    onOpenHolding: () -> Unit,
    onLogout: () -> Unit,
) {
    Card {
        // 顶部：交易账号 + 退出
        Row(
            Modifier.fillMaxWidth().padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(MockData.MINE_TRADE_ACCOUNT, fontSize = 15.sp, color = ZxColors.TextSecondary, fontWeight = FontWeight.Medium)
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.weight(1f))
            Text("退出", fontSize = 15.sp, color = ZxColors.TextSecondary, modifier = Modifier.clickable { onLogout() })
        }

        // 标题行：我的资产 + 眼睛 + 刚刚更新 + 银证转账 + 币种
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("我的资产", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
            Spacer(Modifier.width(5.dp))
            Icon(ZxIcons.Eye, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text("刚刚更新", fontSize = 11.sp, color = ZxColors.TextTertiary)
            Icon(Icons.Outlined.History, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .background(ZxColors.Brand, RoundedCornerShape(50))
                    .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("银证转账", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
                    Icon(Icons.Outlined.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .background(ZxColors.ChipBg, RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text("人民币", fontSize = 11.sp, color = ZxColors.TextSecondary, maxLines = 1, softWrap = false)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 总资产 + 昨日收益
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("人民币总资产(元)", fontSize = 12.sp, color = ZxColors.TextSecondary)
                Spacer(Modifier.height(6.dp))
                Text(money(summary.totalAsset), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("昨日收益(元)", fontSize = 12.sp, color = ZxColors.TextSecondary)
                Spacer(Modifier.height(6.dp))
                Text(money(summary.todayProfit), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pnlColor(summary.todayProfit), maxLines = 1, softWrap = false)
            }
        }

        Spacer(Modifier.height(18.dp))
        // 点“股票 / 当日参考盈亏”行进入持仓（委托交易）页。
        AssetRow("股票", money(summary.marketValue), "当日参考盈亏", money(summary.todayProfit), pnlColor(summary.todayProfit), onClick = onOpenHolding)
        ThinDivider()
        AssetRow("理财", money(0.0), "持有收益", money(0.0), ZxColors.TextPrimary)
        ThinDivider()
        AssetRow("现金", money(summary.cash), "可用资金", money(summary.cash), ZxColors.TextPrimary)
    }
}

@Composable
private fun AssetRow(
    leftLabel: String, leftValue: String,
    rightLabel: String, rightValue: String, rightColor: Color,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(leftLabel, fontSize = 14.sp, color = ZxColors.TextPrimary, modifier = Modifier.width(56.dp))
        Text(
            leftValue, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            color = ZxColors.TextPrimary, modifier = Modifier.weight(1f)
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(rightLabel, fontSize = 12.sp, color = ZxColors.TextSecondary)
            Spacer(Modifier.height(2.dp))
            Text(rightValue, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = rightColor)
        }
        Icon(Icons.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(16.dp))
    }
}

/* ----------------------------- 资产分析 / 月账单 ----------------------------- */

@Composable
private fun AnalysisRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        // 资产分析
        Column(
            Modifier
                .weight(1f)
                .background(ZxColors.CardBg, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("资产分析", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
                Icon(Icons.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "截至06-30收盘，近1个月在同资产段收益",
                fontSize = 12.sp, color = ZxColors.TextSecondary, lineHeight = 17.sp
            )
            Spacer(Modifier.height(12.dp))
            Row {
                Text("战胜", fontSize = 14.sp, color = ZxColors.TextPrimary)
                Text(MockData.MINE_BEAT_PCT, fontSize = 14.sp, color = ZxColors.Brand, fontWeight = FontWeight.Bold)
                Text("投资者", fontSize = 14.sp, color = ZxColors.TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            // 红绿分段进度条
            Row(Modifier.fillMaxWidth().height(4.dp)) {
                Box(Modifier.weight(0.54f).fillMaxWidth().height(4.dp).background(ZxColors.Up, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(3.dp))
                Box(Modifier.weight(0.46f).fillMaxWidth().height(4.dp).background(ZxColors.Down, RoundedCornerShape(2.dp)))
            }
        }
        Spacer(Modifier.width(10.dp))
        // 月账单
        Column(
            Modifier
                .weight(1f)
                .background(ZxColors.CardBg, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("6月账单", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
                Spacer(Modifier.width(4.dp))
                Text("截至06-30", fontSize = 10.sp, color = ZxColors.TextTertiary)
                Icon(Icons.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("收益(元)", fontSize = 12.sp, color = ZxColors.TextSecondary)
                Spacer(Modifier.weight(1f))
                Text(MockData.MINE_MONTH_PROFIT, fontSize = 14.sp, color = ZxColors.Down, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("收益率", fontSize = 12.sp, color = ZxColors.TextSecondary)
                Spacer(Modifier.weight(1f))
                Text(MockData.MINE_MONTH_PCT, fontSize = 14.sp, color = ZxColors.Down, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            MiniLineChart(Modifier.fillMaxWidth().height(36.dp))
        }
    }
}

/** 月账单里的迷你折线图。 */
@Composable
private fun MiniLineChart(modifier: Modifier) {
    val points = listOf(0.5f, 0.55f, 0.62f, 0.78f, 0.9f, 0.72f, 0.6f, 0.7f, 0.82f, 0.7f, 0.5f, 0.55f)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val step = w / (points.size - 1)
        val path = Path()
        points.forEachIndexed { i, v ->
            val x = step * i
            val y = h - v * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, ZxColors.Down, style = Stroke(width = 2.5f))
    }
}

/* ----------------------------- 我的营业部 ----------------------------- */

@Composable
private fun BranchCard() {
    Column(
        Modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth()
            .background(ZxColors.CardBg, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("我的营业部", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            Text("联系电话", fontSize = 14.sp, color = ZxColors.LinkBlue)
            Icon(Icons.Outlined.KeyboardArrowRight, null, tint = ZxColors.LinkBlue, modifier = Modifier.size(16.dp))
        }
    }
}

/* ----------------------------- 复用小组件 ----------------------------- */

@Composable
private fun Card(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .background(ZxColors.CardBg, RoundedCornerShape(12.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun ThinDivider() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(ZxColors.Divider))
}

/* ----------------------------- 宫格区 ----------------------------- */

private data class GridEntry(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color = ZxColors.TextPrimary
)

private val businessItems = listOf(
    GridEntry("手机开户", ZxIcons.OpenAccount, ZxColors.Brand),
    GridEntry("账户信息", ZxIcons.AccountInfo),
    GridEntry("投资者问卷回访", ZxIcons.Survey, ZxColors.Brand),
    GridEntry("我的权限", ZxIcons.Shield, ZxColors.Brand),
)

private val serviceItems = listOf(
    GridEntry("我的订阅", ZxIcons.Bookmark),
    GridEntry("已购工具", ZxIcons.Tools, ZxColors.Brand),
    GridEntry("浏览历史", ZxIcons.History),
    GridEntry("权益专区", ZxIcons.Diamond, ZxColors.Brand),
)

private val featureItems = listOf(
    GridEntry("增值专区", ZxIcons.Trending, ZxColors.Brand),
    GridEntry("模拟交易", ZxIcons.Simulate, ZxColors.Brand),
    GridEntry("智能盯盘", ZxIcons.Alarm, ZxColors.Brand),
    GridEntry("综合账户", ZxIcons.AccountPerson),
)

/** 理财/股票快捷交易（买入/卖出/持仓/交易记录）。 */
@Composable
private fun QuickTradeSection() {
    Column(
        Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .background(ZxColors.CardBg, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("理财快捷交易", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
            Spacer(Modifier.width(20.dp))
            Text("股票快捷交易", fontSize = 16.sp, color = ZxColors.TextSecondary)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(24.dp).height(3.dp).background(ZxColors.TextPrimary, RoundedCornerShape(2.dp)))
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            BuySellCell("买入", "买", ZxColors.Brand, Modifier.weight(1f))
            BuySellCell("卖出", "卖", ZxColors.Brand, Modifier.weight(1f))
            IconCell(GridEntry("持仓", ZxIcons.Holding, ZxColors.Brand), Modifier.weight(1f))
            IconCell(GridEntry("交易记录", ZxIcons.TradeRecord), Modifier.weight(1f))
        }
    }
}

/** 买入/卖出：圆环 + 中间红字。 */
@Composable
private fun BuySellCell(label: String, char: String, ring: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(36.dp).background(Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // 圆环
            Canvas(Modifier.size(36.dp)) {
                drawCircle(color = ring, radius = size.minDimension / 2 - 1.5f, style = Stroke(width = 2.6f))
            }
            Text(char, color = ring, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = ZxColors.TextPrimary)
    }
}

@Composable
private fun IconCell(entry: GridEntry, modifier: Modifier) {
    Column(
        modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(entry.icon, entry.label, tint = entry.tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            entry.label, fontSize = 12.sp, color = ZxColors.TextPrimary,
            maxLines = 2, lineHeight = 15.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** 通用宫格块：标题 + 4 列图标。 */
@Composable
private fun GridSection(title: String, items: List<GridEntry>, showArrow: Boolean = false) {
    Column(
        Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .background(ZxColors.CardBg, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZxColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            if (showArrow) {
                Icon(Icons.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            items.forEach { IconCell(it, Modifier.weight(1f)) }
        }
    }
}
