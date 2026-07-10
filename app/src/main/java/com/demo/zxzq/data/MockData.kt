package com.demo.zxzq.data

/**
 * 持仓证券（静态部分）。持股数与成本为演示 mock，现价由实时行情驱动。
 * code 采用新浪格式，如 sh601658。
 */
data class Holding(
    val code: String,     // 行情代码，如 sh601658
    val name: String,     // 证券名称
    val shares: Long,      // 持股数量（可用=持仓，演示简化）
    val cost: Double,      // 成本价
)

/** 实时行情：现价 + 昨收。 */
data class Quote(
    val code: String,
    val price: Double,     // 现价
    val prevClose: Double, // 昨收
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
    /** 当日参考盈亏 = (现价 - 昨收) * 股数。 */
    val todayProfit: Double get() = if (prevClose > 0) (price - prevClose) * holding.shares else 0.0
}

/** 金额千分位格式化，如 122335.0 → 122,335.00；负数带 -。四舍五入用 HALF_UP（与券商习惯一致）。 */
fun money(v: Double): String {
    val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    nf.roundingMode = java.math.RoundingMode.HALF_UP
    return nf.format(v)
}

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

    /** 可用现金（演示固定），交易页 + 我的页共用。 */
    // 65397.32 - 39597.00（6700 股农行 @5.91 买入）= 25800.32
    const val CASH_AVAILABLE = 25800.32

    // 持股数与成本为演示 mock；现价拉真实行情。
    val holdings = listOf(
        Holding("sh601658", "邮储银行", shares = 10000, cost = 5.0015),
        Holding("sh601988", "中国银行", shares = 6500, cost = 5.4383),
        Holding("sh601288", "农业银行", shares = 12700, cost = 6.2464),
    )

    /** 由持仓视图汇总账户资产（两页共用）。 */
    fun summarize(views: List<HoldingView>): AccountSummary = AccountSummary(
        marketValue = views.sumOf { it.marketValue },
        floatProfit = views.sumOf { it.profit },
        todayProfit = views.sumOf { it.todayProfit },
        cash = CASH_AVAILABLE,
    )

    // ---- 我的页 ----
    const val MINE_PHONE = "136****4527"
    const val MINE_TRADE_ACCOUNT = "88****6458"

    // 资产分析 / 月账单（暂无数据来源，保留 mock 常量）
    const val MINE_BEAT_PCT = "53.89%"
    const val MINE_MONTH_PROFIT = "-2,907.79"
    const val MINE_MONTH_PCT = "-2.34%"

    // ---- 交割单（图3）流水 mock ----
    val statements = listOf(
        StatementItem(
            title = "股息入帐", date = "2026-05-12", amount = "780", balance = "14495.11",
            turnover = "780", fee = "0", stampTax = "0", transferFee = "0",
            surcharge = "0", clearingFee = "0.00",
            secName = "农业银行", secCode = "601288", dealPrice = "6.78", dealQty = "0.00", secBalance = "6000.00"
        ),
        StatementItem(
            title = "证券买入", date = "2026-03-09", amount = "-50014.77", balance = "23712.64",
            turnover = "50000", fee = "14.27", stampTax = "0", transferFee = "0.5",
            surcharge = "0", clearingFee = "0.00",
            secName = "邮储银行", secCode = "601658", dealPrice = "5", dealQty = "10000.00", secBalance = "10000.00"
        ),
        StatementItem(
            title = "证券卖出", date = "2026-02-06", amount = "10614.58", balance = "22727.41",
            turnover = "10620", fee = "5.42", stampTax = "0", transferFee = "0",
            surcharge = "0", clearingFee = "0.00",
            secName = "中国银行", secCode = "601988", dealPrice = "5.31", dealQty = "2000.00", secBalance = "4500.00"
        ),
    )
}
