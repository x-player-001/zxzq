package com.demo.zxzq.data

/**
 * 持仓证券（静态部分）。持股数与成本为演示 mock，现价由实时行情驱动。
 * code 采用新浪格式，如 sh601658。
 */
data class Holding(
    val code: String,     // 行情代码，如 sh601658
    val name: String,     // 证券名称
    val shares: Long,      // 持股数量（可用=持仓，演示简化）
    val cost: Double,      // 成本价（含佣金/过户费摊入）
    val firstBuyDate: String = "",     // 首次买入日 yyyy-MM-dd，用于卖出时算持股时长
    val untaxedDividend: Double = 0.0, // 已收未完税分红累计，卖出时按持股时长补税
    // ---- 生命周期累积（用于清仓时固化已清仓盈亏）----
    val boughtShares: Long = 0,          // 累计买入股数（不随卖出减少）
    val buyCost: Double = 0.0,           // 累计买入成本（含费）
    val buyTurnover: Double = 0.0,       // 累计买入纯成交额（不含费）→ 反推无费用成本价
    val sellProceeds: Double = 0.0,      // 累计卖出净所得（已扣税费）
    val dividendTotal: Double = 0.0,     // 累计分红（税前入账额）
    val todayBought: Long = 0,           // 今日买入股数（重放时相对 today 临时算，跨日自动归 0）→ T+1 可用
    val todayBuyCost: Double = 0.0,      // 今日买入纯成交额累计（算当日盈亏今买部分，跨日归 0）
)

/** 实时行情：现价 + 昨收 + 名称。 */
data class Quote(
    val code: String,
    val price: Double,     // 现价
    val prevClose: Double, // 昨收
    val name: String = "", // 证券名称（新浪返回体第 0 字段）
)

/** 交割单流水条目（图3）。 */
data class StatementItem(
    val title: String,      // 业务名称，如 证券买入
    val date: String,       // 发生日期
    val amount: String,     // 发生金额
    val balance: String,    // 资金余额
    val turnover: String,   // 成交金额
    val fee: String,        // 手续费
    val stampTax: String,   // 印花税
    val transferFee: String,// 过户费
    val surcharge: String,  // 附加费
    val clearingFee: String,// 交易所清算费
    val secName: String,    // 证券名称
    val secCode: String,    // 证券代码
    val dealPrice: String,  // 成交价格
    val dealQty: String,    // 成交数量
    val secBalance: String, // 证券余额
)

/** 持仓 + 行情合并后的展示数据（市值、盈亏均由现价计算）。 */
data class HoldingView(
    val holding: Holding,
    val price: Double,      // 现价（无行情时用成本价兜底）
    val prevClose: Double,  // 昨收（算当日盈亏用；无行情时为 0）
    val hasQuote: Boolean,  // 是否已拿到实时行情
) {
    val marketValue: Double get() = price * holding.shares
    val profit: Double get() = (price - holding.cost) * holding.shares
    val profitPct: Double get() = if (holding.cost == 0.0) 0.0 else (price / holding.cost - 1) * 100
    val isUp: Boolean get() = profit >= 0
    /**
     * 当日参考盈亏（持仓部分 = 老仓 + 今买）：
     * 老仓 (现价−昨收)×昨仓股数 + 今买 (现价−今日买入均价)×今日买入股数。
     * 今卖部分不在持仓里，由 AccountSummary 从 todaySells 另加。
     */
    val todayProfit: Double get() {
        if (prevClose <= 0) return 0.0
        val tb = holding.todayBought
        val oldShares = (holding.shares - tb).coerceAtLeast(0L)
        val oldPart = (price - prevClose) * oldShares
        val buyAvg = if (tb > 0) holding.todayBuyCost / tb else 0.0
        val buyPart = if (tb > 0) (price - buyAvg) * tb else 0.0
        return oldPart + buyPart
    }
    /** T+1 可用股数 = 持仓 − 今日买入（下限 0）。跨日后 todayBought 归 0，可用恢复=持仓。 */
    val availableShares: Long get() = (holding.shares - holding.todayBought).coerceAtLeast(0L)

    /**
     * 无费用成本价：把摊进 cost 的佣金/过户费剔除后的纯成交均价。
     * 由生命周期累积反推——buyCost 含费、buyTurnover 不含费，两者比值即费用摊薄系数。
     * 无累积数据（理论上不会出现）时退回 cost，避免除零。
     */
    val costNoFee: Double get() =
        if (holding.boughtShares == 0L || holding.buyCost == 0.0) holding.cost
        else holding.cost * (holding.buyTurnover / holding.buyCost)

    /** 无费用盈亏 = (现价 − 无费用成本价) × 股数。剔除交易成本后的"纸面"盈亏。 */
    val profitNoFee: Double get() = (price - costNoFee) * holding.shares

    /** 无费用盈亏比例。 */
    val profitNoFeePct: Double get() =
        if (costNoFee == 0.0) 0.0 else (price / costNoFee - 1) * 100

    /** 当日盈亏比例：相对昨日市值（老仓口径）。 */
    val todayProfitPct: Double get() {
        val prev = marketValue - todayProfit
        return if (prev != 0.0) todayProfit / prev * 100 else 0.0
    }
}

/** 金额千分位格式化，如 122335.0 → 122,335.00；负数带 -。四舍五入用 HALF_UP（与券商习惯一致）。 */
fun money(v: Double): String {
    val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    nf.roundingMode = java.math.RoundingMode.HALF_UP
    return nf.format(v)
}

/** 不带千分位的两位小数，如 59994.0 → "59994.00"。仅持仓行用（对齐真实 App）。 */
fun plain(v: Double): String = String.format(java.util.Locale.US, "%.2f", v)

/**
 * 账户资产汇总：由持仓行情 + 可用现金计算得出，交易页与我的页共用同一套数字。
 */
data class AccountSummary(
    val marketValue: Double,   // 股票总市值 = Σ 现价×股数
    val floatProfit: Double,   // 浮动盈亏  = Σ (现价-成本)×股数
    val todayProfit: Double,   // 当日盈亏  = Σ (现价-昨收)×股数
    val cash: Double,          // 可用现金
) {
    val totalAsset: Double get() = marketValue + cash          // 总资产
    /** 仓位 = 股票市值 / 总资产 * 100。 */
    val positionRate: Double get() = if (totalAsset != 0.0) marketValue / totalAsset * 100 else 0.0
    /** 当日涨跌% ≈ 当日盈亏 / 昨日市值。 */
    val todayPct: Double get() {
        val prev = marketValue - todayProfit
        return if (prev != 0.0) todayProfit / prev * 100 else 0.0
    }
}

object MockData {

    // ---- 委托交易 / 持仓页 ----
    const val TRADE_NAME = "郭*亚"
    const val TRADE_CLIENT_NO = "88****0212"

    // 持仓 / 可用现金 / 交割单流水已迁移到 TradeRepository（由 SeedTrades 引擎重放生成）。

    /** 由持仓视图 + 可用现金汇总账户资产（三页共用）。cash 来自 TradeRepository 实时状态。 */
    fun summarize(
        views: List<HoldingView>,
        cash: Double,
        todaySells: List<TodaySell> = emptyList(),
    ): AccountSummary = AccountSummary(
        marketValue = views.sumOf { it.marketValue },
        floatProfit = views.sumOf { it.profit },
        // 当日盈亏 = 持仓部分(老仓+今买) + 今卖部分(卖价−昨收)×股数
        todayProfit = views.sumOf { it.todayProfit } +
            todaySells.sumOf { (it.price - it.prevClose) * it.qty },
        cash = cash,
    )

    // ---- 我的页 ----
    const val MINE_PHONE = "136****4527"
    const val MINE_TRADE_ACCOUNT = "88****6458"

    // 资产分析 / 月账单（暂无数据来源，保留 mock 常量）
    const val MINE_BEAT_PCT = "53.89%"
    const val MINE_MONTH_PROFIT = "-2,907.79"
    const val MINE_MONTH_PCT = "-2.34%"

}
