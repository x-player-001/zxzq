package com.demo.zxzq.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.zxzq.data.HoldingView
import com.demo.zxzq.data.MockData
import com.demo.zxzq.data.Quote
import com.demo.zxzq.data.Order
import com.demo.zxzq.data.OrderResult
import com.demo.zxzq.data.OrderSide
import com.demo.zxzq.data.OrderStatus
import com.demo.zxzq.data.QuoteRepository
import com.demo.zxzq.data.TradeRepository
import com.demo.zxzq.data.TradeState
import com.demo.zxzq.data.money
import com.demo.zxzq.data.plain
import com.demo.zxzq.ui.icons.ZxIcons
import com.demo.zxzq.ui.theme.ZxColors
import com.demo.zxzq.ui.theme.NumberFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TradeScreen(onBack: () -> Unit = {}, onOpenStatement: () -> Unit = {}, onOpenClosed: () -> Unit = {}) {
    var tab by remember { mutableIntStateOf(3) } // 默认 持仓

    // 交易数据（持仓 + 可用现金 + 委托单）来自可观察仓库，下单成交后本页联动。
    val tradeState by TradeRepository.state.collectAsState()
    val holdings = tradeState.holdings
    val codes = holdings.map { it.code }

    // 实时行情：每 1 分钟拉一次新浪行情。codes 变化时重启轮询。
    var quotes by remember { mutableStateOf<Map<String, Quote>>(emptyMap()) }
    var refreshing by remember { mutableStateOf(false) }

    suspend fun refresh() {
        refreshing = true
        val q = QuoteRepository.fetch(codes)
        if (q.isNotEmpty()) {
            quotes = q
            TradeRepository.updateQuotes(q)  // 缓存行情供卖出时抓昨收
        }
        refreshing = false
    }

    LaunchedEffect(codes) {
        while (true) {
            refresh()
            delay(60_000)
        }
    }

    // 合成持仓视图（现价缺失时用成本价兜底）。
    val views = QuoteRepository.toViews(holdings, quotes)
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(ZxColors.ScreenBg)) {
        TradeTopBar(onBack = onBack)
        TradeTabs(selected = tab, onSelect = { tab = it })
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(ZxColors.Divider))
        when (tab) {
            0 -> OrderForm(OrderSide.BUY, tradeState, quotes)
            1 -> OrderForm(OrderSide.SELL, tradeState, quotes)
            2 -> CancelBody(tradeState.orders)
            3 -> HoldingBody(
                views = views, cash = tradeState.cash, todaySells = tradeState.todaySells,
                refreshing = refreshing, onRefresh = { scope.launch { refresh() } },
                onOpenClosed = onOpenClosed,
            )
            4 -> TradeQueryContent(onOpenStatement = onOpenStatement)
        }
    }
}

private fun pct(v: Double): String = String.format("%+.2f%%", v)

/* ----------------------------- 持仓页 body ----------------------------- */

@Composable
private fun HoldingBody(
    views: List<HoldingView>,
    cash: Double,
    todaySells: List<com.demo.zxzq.data.TodaySell>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenClosed: () -> Unit,
) {
    // 表头与所有数据行共用一个横向滚动状态 → 滑动完全同步。
    val hScroll = rememberScrollState()
    var sort by remember { mutableStateOf(SortState()) }

    // 列宽 = 滑动视口宽 / 一屏列数；视口 = 整行宽 − 固定列宽。
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rowWidth = maxWidth - ROW_EDGE_PADDING           // 减去行左内边距
        val pinnedWidth = rowWidth * PINNED_FRACTION
        val scrollViewport = rowWidth - pinnedWidth
        val colWidth = scrollViewport / VISIBLE_COLS
        val density = LocalDensity.current
        val colWidthPx = with(density) { colWidth.toPx() }
        val maxOffsetPx = with(density) {
            ((colWidth * SCROLL_COLS.size) - scrollViewport).coerceAtLeast(0.dp).toPx()
        }
        val fling = remember(colWidthPx, maxOffsetPx) {
            SnapColumnFling(hScroll, colWidthPx, maxOffsetPx)
        }

        val sorted = remember(views, sort) { views.applySort(sort) }
        val totalMv = remember(views) { views.sumOf { it.marketValue } }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { AssetSummary(views, cash, todaySells) }
            item { Spacer(Modifier.height(3.dp).fillMaxWidth().background(ZxColors.ScreenBg)) }
            item { HoldingHeader() }
            item { CategoryChips(count = views.size) }
            item {
                ColumnTitles(
                    sort = sort, onSort = { sort = sort.toggle(it) },
                    scrollState = hScroll, fling = fling,
                    pinnedWidth = pinnedWidth, colWidth = colWidth
                )
            }
            item { AccountLine(count = views.size) }
            items(sorted, key = { it.holding.code }) {
                HoldingRow(
                    v = it, totalMarketValue = totalMv,
                    scrollState = hScroll, fling = fling,
                    pinnedWidth = pinnedWidth, colWidth = colWidth
                )
            }
            item { FooterLinks(refreshing = refreshing, onRefresh = onRefresh, onOpenClosed = onOpenClosed) }
        }
    }
}

/* ----------------------------- 顶部标题栏 ----------------------------- */

@Composable
private fun TradeTopBar(onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(ZxColors.CardBg)
            .padding(horizontal = 12.dp, vertical = 7.dp)
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
            Text("委托交易", fontSize = 19.sp, color = ZxColors.TextPrimary, lineHeight = 21.sp)
            Text(
                "${MockData.TRADE_NAME}  客户号:${MockData.TRADE_CLIENT_NO}",
                fontSize = 11.sp, color = ZxColors.TextSecondary, lineHeight = 13.sp
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
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(vertical = 6.dp)
    ) {
        tabs.forEachIndexed { i, t ->
            val isSel = i == selected
            Column(
                Modifier.weight(1f).clickable { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    t,
                    fontSize = 15.sp,
                    lineHeight = 16.sp,
                    color = ZxColors.TextPrimary,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(Modifier.height(4.dp))
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
private fun AssetSummary(views: List<HoldingView>, cash: Double, todaySells: List<com.demo.zxzq.data.TodaySell>) {
    // 实时汇总：总市值 / 浮动盈亏 / 当日参考盈亏 / 总资产（与我的页共用同一套计算）。
    val s = MockData.summarize(views, cash, todaySells)
    val available = s.cash
    val marketValue = s.marketValue
    val floatProfit = s.floatProfit
    val todayProfit = s.todayProfit
    val totalAsset = s.totalAsset
    val todayPct = s.todayPct

    Column(Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 16.dp, vertical = 12.dp)) {
        // 人民币账户 A股 + 仓位
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🇨🇳", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text("人民币账户 A股", fontSize = 15.sp, color = ZxColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.background(ZxColors.ChipBg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(String.format("仓位%.2f%%", s.positionRate), fontSize = 13.sp, color = ZxColors.TextSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(ZxColors.Divider))
        Spacer(Modifier.height(12.dp))

        // 第一行：资产 / 浮动盈亏 / 当日参考盈亏
        Row(Modifier.fillMaxWidth()) {
            MetricCol(
                modifier = Modifier.weight(1f),
                label = "资产", labelIcon = MetricIcon.Eye,
                value = plain(totalAsset), valueColor = ZxColors.TextPrimary, arrow = true
            )
            MetricCol(
                modifier = Modifier.weight(1f),
                label = "浮动盈亏", labelIcon = MetricIcon.Help,
                value = plain(floatProfit), valueColor = pnlColor(floatProfit)
            )
            MetricCol(
                modifier = Modifier.weight(1f),
                label = "当日参考盈亏", labelIcon = MetricIcon.Help,
                value = plain(todayProfit), valueColor = pnlColor(todayProfit),
                suffix = pct(todayPct), arrow = true
            )
        }

        Spacer(Modifier.height(10.dp))

        // 第二行：总市值 / 可用(逆回购) / 可取(转账)
        Row(Modifier.fillMaxWidth()) {
            MetricCol(
                modifier = Modifier.weight(1f),
                label = "总市值", value = plain(marketValue), valueColor = ZxColors.TextPrimary
            )
            Column(Modifier.weight(1f)) {
                LabelWithTag("可用", "逆回购")
                Spacer(Modifier.height(4.dp))
                Text(plain(available), fontSize = 16.sp, fontFamily = NumberFont, color = ZxColors.TextPrimary, maxLines = 1, softWrap = false, lineHeight = 18.sp)
            }
            Column(Modifier.weight(1f)) {
                LabelWithTag("可取", "转账")
                Spacer(Modifier.height(4.dp))
                Text(plain(available), fontSize = 16.sp, fontFamily = NumberFont, color = ZxColors.TextPrimary, maxLines = 1, softWrap = false, lineHeight = 18.sp)
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
            Text(label, fontSize = 12.sp, color = ZxColors.TextSecondary, maxLines = 1, softWrap = false, lineHeight = 13.sp)
            Spacer(Modifier.width(3.dp))
            when (labelIcon) {
                MetricIcon.Eye -> Icon(ZxIcons.Eye, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(12.dp))
                MetricIcon.Help -> Icon(Icons.AutoMirrored.Outlined.HelpOutline, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(12.dp))
                MetricIcon.None -> {}
            }
        }
        Spacer(Modifier.height(4.dp))
        // 主数字用基线对齐，保证三列在同一水平线（suffix/arrow 不影响主数字位置）。
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 16.sp, fontFamily = NumberFont, color = valueColor, maxLines = 1, softWrap = false, lineHeight = 18.sp, modifier = Modifier.alignByBaseline())
            if (suffix != null) {
                Spacer(Modifier.width(3.dp))
                Text(suffix, fontSize = 11.sp, fontFamily = NumberFont, color = valueColor, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline())
            }
            if (arrow) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun LabelWithTag(label: String, tag: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = ZxColors.TextSecondary, lineHeight = 13.sp)
        Icon(Icons.AutoMirrored.Outlined.HelpOutline, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        // 逆回购/转账红底标签：更紧凑，贴近真实 App
        Box(
            Modifier.background(ZxColors.ChipBgRedLight, RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(tag, fontSize = 10.sp, color = ZxColors.Brand, lineHeight = 13.sp)
        }
    }
}

/* ----------------------------- 持仓证券头 ----------------------------- */

@Composable
private fun HoldingHeader() {
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("持仓证券", fontSize = 15.sp, color = ZxColors.TextPrimary)
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
private fun CategoryChips(count: Int) {
    // 数字随实际持仓数联动（demo 持仓都是股票）。
    val chips = listOf("全部($count)" to true, "股票($count)" to false, "债券(0)" to false, "基金(0)" to false, "其他(0)" to false)
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chips.forEach { (text, sel) ->
            Box(
                Modifier
                    .weight(1f)
                    .background(if (sel) ZxColors.ChipBgRedLight else ZxColors.ChipBg, RoundedCornerShape(6.dp))
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text, fontSize = 14.sp,
                    color = if (sel) ZxColors.Brand else ZxColors.TextPrimary,  // 未选中深灰（真实 #2B2B2B）
                    fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1, softWrap = false
                )
            }
        }
    }
}

/* ----------------------------- 可横滑列体系 ----------------------------- */

/**
 * 持仓列表的滑动列定义。第一列「市值」固定不动，其余列在共享视口里整体横滑。
 * 每列宽度相同（= 视口宽 / VISIBLE_COLS），滑动带磁吸，正好停在整列边界。
 */
private enum class HoldingCol(val title: String) {
    MARKET_VALUE("市值"),   // 固定列，不参与横滑，但同样可排序
    PROFIT("盈亏"),
    SHARES("持仓/ 可用"),
    COST("成本/ 市价"),
    TODAY("当日盈亏"),
    WEIGHT("个股仓位"),
    NOFEE("无费用盈亏"),
    NAME_CODE("名称/代码"),
}

/** 参与横滑的列（去掉固定的市值列）。 */
private val SCROLL_COLS = HoldingCol.entries.filter { it != HoldingCol.MARKET_VALUE }

/** 滑动区一屏显示的列数（真实图：盈亏/持仓可用/成本市价 三列）。 */
private const val VISIBLE_COLS = 3

/** 排序方向：无（三个点）→ 降序 → 升序 → 无。 */
private enum class SortDir { NONE, DESC, ASC }

/**
 * 排序标记槽位的固定尺寸。三个点竖排实际高 2+1.5+2+1.5+2 = 9dp，
 * 槽位取 12dp 略放宽以容下 11sp 的箭头字形（避免被裁切）。
 * 锁死宽高后三态切换表头不会重排（宽度跳动 / 行被撑高）。
 */
private val SORT_MARK_WIDTH = 9.dp
private val SORT_MARK_HEIGHT = 12.dp

/** 当前排序状态：按哪一列、什么方向。col=null 表示默认顺序（按市值降序）。 */
private data class SortState(val col: HoldingCol? = null, val dir: SortDir = SortDir.NONE)

/** 点击表头循环切换：同列 无→降→升→无；换列则从降序开始。 */
private fun SortState.toggle(c: HoldingCol): SortState =
    if (col != c) SortState(c, SortDir.DESC)
    else when (dir) {
        SortDir.DESC -> SortState(c, SortDir.ASC)
        SortDir.ASC -> SortState(null, SortDir.NONE)
        SortDir.NONE -> SortState(c, SortDir.DESC)
    }

/** 按排序状态给持仓排序。未指定列时按市值降序（真实 App 默认）。 */
private fun List<HoldingView>.applySort(s: SortState): List<HoldingView> {
    // 默认（含"恢复三个点"）：按市值降序，与真实 App 一致。
    if (s.col == null || s.dir == SortDir.NONE) return sortedByDescending { it.marketValue }
    val asc = s.dir == SortDir.ASC
    // 名称列按代码字典序，其余列按数值。
    if (s.col == HoldingCol.NAME_CODE) {
        return if (asc) sortedBy { it.holding.code } else sortedByDescending { it.holding.code }
    }
    val key: (HoldingView) -> Double = when (s.col) {
        HoldingCol.MARKET_VALUE -> { v -> v.marketValue }
        HoldingCol.PROFIT -> { v -> v.profit }
        HoldingCol.SHARES -> { v -> v.holding.shares.toDouble() }
        HoldingCol.COST -> { v -> v.holding.cost }
        HoldingCol.TODAY -> { v -> v.todayProfit }
        HoldingCol.WEIGHT -> { v -> v.marketValue }
        HoldingCol.NOFEE -> { v -> v.profitNoFee }
        else -> { v -> v.marketValue }
    }
    return if (asc) sortedBy(key) else sortedByDescending(key)
}

/**
 * 磁吸 FlingBehavior：抬手后按滑动速度决定进/退一列，停在整列边界。
 * 速度超过阈值就顺势翻一列，否则回弹到最近的一列。
 */
private class SnapColumnFling(
    private val scrollState: ScrollState,
    private val colWidthPx: Float,
    private val maxOffsetPx: Float,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (colWidthPx <= 0f || maxOffsetPx <= 0f) return initialVelocity
        val current = scrollState.value.toFloat()
        val idx = current / colWidthPx
        // 手指左滑 → 速度为负 → offset 变大（前进一列）；右滑相反。
        val target = when {
            initialVelocity <= -FLING_THRESHOLD -> kotlin.math.floor(idx) + 1f
            initialVelocity >= FLING_THRESHOLD -> kotlin.math.ceil(idx) - 1f
            else -> kotlin.math.round(idx)
        }
        val dest = (target * colWidthPx).coerceIn(0f, maxOffsetPx)
        // 吸附是"走到指定位置"，不再把甩动速度灌进动画，否则会冲过目标列。
        var last = current
        animate(
            initialValue = current,
            targetValue = dest,
            animationSpec = tween(260, easing = FastOutSlowInEasing),
        ) { value, _ ->
            scrollBy(value - last)
            last = value
        }
        return 0f
    }

    private companion object {
        /** 甩动速度阈值（px/s），超过就顺势翻一整列。 */
        const val FLING_THRESHOLD = 400f
    }
}

/**
 * 表头排序标记：无排序显示三个点（⋮ 横排），有排序显示红色箭头。
 * 点击整个表头单元格切换排序。
 */
@Composable
private fun SortMark(dir: SortDir) {
    // 三态共用一个固定尺寸的槽位：三个点 / ↓ / ↑ 切换时表头宽高都不变。
    // （箭头用 Text 会带上字体行高，比三个点高，不锁尺寸就会把整行撑高。）
    Box(
        Modifier.padding(start = 3.dp).size(width = SORT_MARK_WIDTH, height = SORT_MARK_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        when (dir) {
            SortDir.NONE -> Column(
                verticalArrangement = Arrangement.spacedBy(1.5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(Modifier.size(2.dp).background(ZxColors.TextTertiary, RoundedCornerShape(1.dp)))
                }
            }
            SortDir.DESC -> Text(
                "↓", fontSize = 11.sp, color = ZxColors.Brand,
                lineHeight = 11.sp, maxLines = 1, softWrap = false
            )
            SortDir.ASC -> Text(
                "↑", fontSize = 11.sp, color = ZxColors.Brand,
                lineHeight = 11.sp, maxLines = 1, softWrap = false
            )
        }
    }
}

/** 一个可点击排序的表头单元格。 */
@Composable
private fun HeaderCell(
    col: HoldingCol,
    sort: SortState,
    onSort: (HoldingCol) -> Unit,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = true,
    /** 默认排序态（无选中列时市值列高亮为降序）。 */
    defaultActive: Boolean = false,
) {
    val active = sort.col == col || defaultActive
    val dir = when {
        sort.col == col -> sort.dir
        defaultActive -> SortDir.DESC
        else -> SortDir.NONE
    }
    Row(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onSort(col) },
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 字重固定为 Medium：选中态只靠颜色区分。
        // 若按选中切 Normal/Medium，字宽会变，表头又会左右跳动。
        // lineHeight 锁死，避免表头行高随字体默认行距浮动。
        Text(
            col.title, fontSize = 12.sp,
            color = if (active) ZxColors.Brand else ZxColors.TextSecondary,
            fontWeight = FontWeight.Medium,
            lineHeight = 14.sp,
            maxLines = 1, softWrap = false
        )
        SortMark(dir)
    }
}

@Composable
private fun AccountLine(count: Int) {
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("人民币账户 A 股", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ZxColors.TextPrimary)
        Spacer(Modifier.weight(1f))
        Text("持有 $count 只", fontSize = 13.sp, color = ZxColors.TextSecondary)
        Icon(Icons.Outlined.KeyboardArrowUp, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(18.dp))
    }
}

/* ----------------------------- 持仓行 ----------------------------- */

/** 固定列（名称/市值）占行宽的比例，与真实图一致（名称列最宽）。 */
private const val PINNED_FRACTION = 0.30f

/** 行左右边距：固定列离左边框、最后一列离右边框都留这么多，视觉对称。 */
private val ROW_EDGE_PADDING = 16.dp

/**
 * 表头行：固定「市值」列 + 可横滑的其余列（与数据行共用 scrollState，滑动完全同步）。
 */
@Composable
private fun ColumnTitles(
    sort: SortState,
    onSort: (HoldingCol) -> Unit,
    scrollState: ScrollState,
    fling: FlingBehavior?,
    pinnedWidth: Dp,
    colWidth: Dp,
) {
    ScrollableRow(
        scrollState = scrollState, fling = fling, pinnedWidth = pinnedWidth, colWidth = colWidth,
        modifier = Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(start = ROW_EDGE_PADDING, top = 8.dp, bottom = 8.dp),
        pinned = {
            // 固定列「市值」同样可排序；默认态（col=null）显示红色降序箭头，与真实图一致。
            HeaderCell(
                col = HoldingCol.MARKET_VALUE, sort = sort, onSort = onSort,
                alignEnd = false,
                defaultActive = sort.col == null,
            )
        },
        cell = { c -> HeaderCell(col = c, sort = sort, onSort = onSort) }
    )
}

/**
 * 表头 + 数据行统一的「固定列 + 滑动列」骨架。
 * pinned 画固定列内容，cell 按列画滑动列内容，二者共用同一 scrollState。
 */
@Composable
private fun ScrollableRow(
    scrollState: ScrollState,
    fling: FlingBehavior?,
    pinnedWidth: Dp,
    colWidth: Dp,
    modifier: Modifier = Modifier,
    pinned: @Composable () -> Unit,
    cell: @Composable (HoldingCol) -> Unit,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // 固定列：宽度由外层按屏宽算好传入，保证表头与数据行严格对齐。
        Box(Modifier.width(pinnedWidth)) { pinned() }
        Row(
            Modifier.horizontalScroll(scrollState, flingBehavior = fling),
        ) {
            SCROLL_COLS.forEach { c ->
                // 列宽保持 colWidth 不变（磁吸按整列算），右内边距在列内部留，
                // 这样最后一列滑到底时也和右边框有间距，与固定列的左边距对称。
                Box(
                    Modifier.width(colWidth).padding(end = ROW_EDGE_PADDING),
                    contentAlignment = Alignment.CenterEnd
                ) { cell(c) }
            }
        }
    }
}

/** 一个列单元格里的上下两行数字（真实图每列都是双行）。 */
@Composable
private fun CellPair(
    top: String, bottom: String,
    topColor: Color, bottomColor: Color,
    topWeight: FontWeight = FontWeight.Normal,
) {
    val lh = 16.sp
    Column(horizontalAlignment = Alignment.End) {
        Text(top, fontSize = 14.sp, fontFamily = NumberFont, fontWeight = topWeight, color = topColor, lineHeight = lh)
        Spacer(Modifier.height(3.dp))
        Text(bottom, fontSize = 14.sp, fontFamily = NumberFont, color = bottomColor, lineHeight = lh)
    }
}

@Composable
private fun HoldingRow(
    v: HoldingView,
    totalMarketValue: Double,
    scrollState: ScrollState,
    fling: FlingBehavior?,
    pinnedWidth: Dp,
    colWidth: Dp,
) {
    val color = if (v.isUp) ZxColors.Up else ZxColors.Down
    // 行高压到接近字号（Compose 默认 lineHeight 远大于字号，是行距偏大的主因）。
    val lh = 16.sp   // 14sp 的行高
    ScrollableRow(
        scrollState = scrollState, fling = fling, pinnedWidth = pinnedWidth, colWidth = colWidth,
        modifier = Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(start = ROW_EDGE_PADDING, top = 12.dp, bottom = 12.dp),
        pinned = {
            // 名称 + 市值（名称是汉字用系统字体，市值数字用 NumberFont）
            Column {
                Text(v.holding.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ZxColors.TextPrimary, lineHeight = lh)
                Spacer(Modifier.height(3.dp))
                Text(plain(v.marketValue), fontSize = 14.sp, fontFamily = NumberFont, color = ZxColors.TextPrimary, lineHeight = lh)
            }
        },
        cell = { c ->
            when (c) {
                // 盈亏 + 比例
                HoldingCol.PROFIT -> CellPair(
                    plain(v.profit), pct(v.profitPct), color, color, FontWeight.Medium
                )
                // 持仓 / 可用（T+1：当天买入不可卖，可用=持仓−今日买入）
                HoldingCol.SHARES -> CellPair(
                    v.holding.shares.toString(), v.availableShares.toString(),
                    ZxColors.TextPrimary, ZxColors.TextSecondary
                )
                // 成本 / 市价
                HoldingCol.COST -> CellPair(
                    String.format("%.4f", v.holding.cost), String.format("%.4f", v.price),
                    ZxColors.TextPrimary, color
                )
                // 当日盈亏 + 比例
                HoldingCol.TODAY -> {
                    val tc = if (v.todayProfit >= 0) ZxColors.Up else ZxColors.Down
                    CellPair(plain(v.todayProfit), pct(v.todayProfitPct), tc, tc, FontWeight.Medium)
                }
                // 个股仓位：该股市值占总持仓市值比（下行留空，保持双行高度一致）
                HoldingCol.WEIGHT -> {
                    val w = if (totalMarketValue > 0) v.marketValue / totalMarketValue * 100 else 0.0
                    CellPair(
                        String.format("%.2f%%", w), "",
                        ZxColors.TextPrimary, ZxColors.TextSecondary
                    )
                }
                // 无费用盈亏 + 比例（剔除佣金/过户费摊入）
                HoldingCol.NOFEE -> {
                    val nc = if (v.profitNoFee >= 0) ZxColors.Up else ZxColors.Down
                    CellPair(plain(v.profitNoFee), pct(v.profitNoFeePct), nc, nc, FontWeight.Medium)
                }
                // 市值是固定列，不会走到滑动区
                HoldingCol.MARKET_VALUE -> Unit
                // 名称/代码：汉字不用 NumberFont，代码用
                HoldingCol.NAME_CODE -> Column(horizontalAlignment = Alignment.End) {
                    Text(v.holding.name, fontSize = 14.sp, color = ZxColors.TextPrimary, lineHeight = lh, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        v.holding.code.dropWhile { !it.isDigit() },
                        fontSize = 14.sp, fontFamily = NumberFont, color = ZxColors.TextSecondary, lineHeight = lh
                    )
                }
            }
        }
    )
}

/* ----------------------------- 在途/已清仓 + 刷新 ----------------------------- */

@Composable
private fun FooterLinks(refreshing: Boolean, onRefresh: () -> Unit, onOpenClosed: () -> Unit) {
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
            FooterRow("已清仓证券", onClick = onOpenClosed)
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
private fun FooterRow(text: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 16.sp, color = ZxColors.TextPrimary)
        Spacer(Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = ZxColors.TextTertiary, modifier = Modifier.size(20.dp))
    }
}

/* ----------------------------- 买入 / 卖出 表单 ----------------------------- */

private val BuyColor = Color(0xFFE4393C)
private val SellColor = Color(0xFF2E6DE6)

@Composable
private fun OrderForm(side: OrderSide, state: TradeState, quotes: Map<String, Quote>) {
    val context = LocalContext.current
    val isBuy = side == OrderSide.BUY
    val accent = if (isBuy) BuyColor else SellColor

    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("") }

    // 手输代码后自动拉行情，回填名称（新浪返回体含名称）+ 现价。
    // 用规范化后的代码（补 sh/sz 前缀），避免 000725 这类无前缀代码拉不到。
    val normCode = QuoteRepository.normalizeCode(code)
    LaunchedEffect(normCode) {
        if (normCode.matches(Regex("(sh|sz)\\d{6}"))) {
            delay(400) // 防抖：输入停顿后再请求
            val q = QuoteRepository.fetch(listOf(normCode))[normCode]
            if (q != null) {
                if (q.name.isNotBlank()) name = q.name
                if (priceText.isBlank()) priceText = String.format("%.2f", q.price)
            }
        } else {
            // 代码不完整时清掉旧名称，避免张冠李戴
            if (state.holdings.none { it.code == normCode }) name = ""
        }
    }

    val price = priceText.toDoubleOrNull() ?: 0.0
    val qty = qtyText.toLongOrNull() ?: 0L

    // 卖出：只能选持仓证券。买入：可从持仓快捷选，也可手输代码。
    val held = state.holdings.find { it.code == QuoteRepository.normalizeCode(code) }
    val sellable = held?.shares ?: 0L
    val maxBuyable = if (price > 0) ((state.cash / price).toLong() / 100) * 100 else 0L

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // 证券代码 / 名称（数字键盘：输 6 位数字，normalizeCode 自动补 sh/sz 前缀）
        OrderInput(
            label = "证券代码",
            value = code,
            onValueChange = { code = it.trim() },
            keyboardType = KeyboardType.Number,
            trailing = if (name.isNotBlank()) name else null,
        )
        Spacer(Modifier.height(10.dp))

        // 快捷选持仓（点一下自动填代码+名称，并带出现价）
        if (state.holdings.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.holdings.forEach { h ->
                    val q = quotes[h.code]
                    Box(
                        Modifier
                            .background(ZxColors.ChipBg, RoundedCornerShape(6.dp))
                            .clickable {
                                code = h.code; name = h.name
                                if (q != null) priceText = String.format("%.2f", q.price)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(h.name, fontSize = 13.sp, color = ZxColors.TextPrimary)
                    }
                }
            }
        }

        // 价格（步进 0.01）
        StepperInput(
            label = "价格", value = priceText, accent = accent,
            onValueChange = { priceText = it },
            onStep = { d -> priceText = String.format("%.2f", (price + d * 0.01).coerceAtLeast(0.0)) }
        )
        Spacer(Modifier.height(10.dp))

        // 数量（步进 100）
        StepperInput(
            label = "数量(股)", value = qtyText, accent = accent,
            onValueChange = { qtyText = it },
            onStep = { d -> qtyText = ((qty + d * 100).coerceAtLeast(0)).toString() }
        )
        Spacer(Modifier.height(12.dp))

        // 可买 / 可卖 提示
        Text(
            if (isBuy) "可用 ${money(state.cash)}   可买 $maxBuyable 股"
            else "持仓 $sellable 股   可卖 $sellable 股",
            fontSize = 13.sp, color = ZxColors.TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "预估金额 ${money(price * qty)}",
            fontSize = 13.sp, color = ZxColors.TextSecondary
        )
        Spacer(Modifier.height(20.dp))

        // 下单按钮
        Box(
            Modifier.fillMaxWidth().height(46.dp)
                .background(accent, RoundedCornerShape(8.dp))
                .clickable {
                    // 卖出时名称从持仓补齐；代码规范化为 sh/sz 前缀
                    val useName = if (name.isBlank()) (held?.name ?: "") else name
                    when (val r = TradeRepository.placeOrder(side, normCode, useName, price, qty)) {
                        is OrderResult.Success -> {
                            Toast.makeText(context, "委托已提交", Toast.LENGTH_SHORT).show()
                            priceText = ""; qtyText = ""
                        }
                        is OrderResult.Failure ->
                            Toast.makeText(context, r.reason, Toast.LENGTH_SHORT).show()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(if (isBuy) "买入下单" else "卖出下单", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OrderInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailing: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailing?.let { { Text(it, fontSize = 14.sp, color = ZxColors.TextPrimary) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ZxColors.Brand,
            cursorColor = ZxColors.Brand,
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StepperInput(
    label: String,
    value: String,
    accent: Color,
    onValueChange: (String) -> Unit,
    onStep: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepBtn("－", accent) { onStep(-1) }
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 13.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZxColors.Brand,
                cursorColor = ZxColors.Brand,
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        StepBtn("＋", accent) { onStep(1) }
    }
}

@Composable
private fun StepBtn(text: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).background(ZxColors.ChipBg, RoundedCornerShape(8.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 20.sp, color = accent, fontWeight = FontWeight.Bold)
    }
}

/* ----------------------------- 撤单页 ----------------------------- */

@Composable
private fun CancelBody(orders: List<Order>) {
    val context = LocalContext.current
    // 只列可操作的「已报」委托，倒序（最新在上）。
    val active = orders.filter { it.status == OrderStatus.REPORTED }.reversed()

    if (active.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无可撤委托", fontSize = 15.sp, color = ZxColors.TextSecondary)
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(active, key = { it.id }) { o ->
            OrderRow(
                order = o,
                onFill = {
                    val r = TradeRepository.fillOrder(o.id)
                    val msg = if (r is OrderResult.Failure) r.reason else "已成交"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onCancel = {
                    val r = TradeRepository.cancelOrder(o.id)
                    val msg = if (r is OrderResult.Failure) r.reason else "已撤单"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
            )
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(ZxColors.Divider))
        }
    }
}

@Composable
private fun OrderRow(order: Order, onFill: () -> Unit, onCancel: () -> Unit) {
    val sideColor = if (order.side == OrderSide.BUY) BuyColor else SellColor
    val sideText = if (order.side == OrderSide.BUY) "买入" else "卖出"
    Row(
        Modifier.fillMaxWidth().background(ZxColors.CardBg).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.background(sideColor, RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(sideText, fontSize = 11.sp, color = Color.White)
                }
                Spacer(Modifier.width(6.dp))
                Text(order.name.ifBlank { order.code }, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ZxColors.TextPrimary)
            }
            Spacer(Modifier.height(4.dp))
            Text("委托价 ${String.format("%.2f", order.price)}   ${order.qty} 股", fontSize = 12.sp, color = ZxColors.TextSecondary)
        }
        // 成交 / 撤单
        Box(
            Modifier.background(BuyColor, RoundedCornerShape(6.dp)).clickable { onFill() }.padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text("成交", fontSize = 13.sp, color = Color.White)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.background(ZxColors.ChipBg, RoundedCornerShape(6.dp)).clickable { onCancel() }.padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text("撤单", fontSize = 13.sp, color = ZxColors.TextPrimary)
        }
    }
}
