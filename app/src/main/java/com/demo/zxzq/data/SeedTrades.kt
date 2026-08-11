package com.demo.zxzq.data

/**
 * 历史种子交易：首次启动时由 TradeRepository 用交易引擎逐笔重放，
 * 自动算出佣金/印花税/过户费/红利税、最终持仓与现金、全部交割单流水。
 * 手工口算的旧数字不再使用——一切以引擎重放结果为准。
 *
 * qty 与 amount 二选一：amount 表示「按金额买入」，引擎按整百股向下取。
 */
sealed interface SeedEvent {
    val date: String

    data class Buy(
        override val date: String,
        val code: String, val name: String, val price: Double,
        val qty: Long? = null, val amount: Double? = null,
    ) : SeedEvent

    data class Sell(
        override val date: String,
        val code: String, val price: Double,
        val qty: Long? = null,          // null = 全部卖出
        val prevClose: Double? = null,  // 卖出当天昨收（算当日盈亏用）；历史 seed 无=null
    ) : SeedEvent

    data class Dividend(
        override val date: String,
        val code: String, val perShare: Double,
    ) : SeedEvent

    /** 银证转入（入金）。 */
    data class Deposit(
        override val date: String,
        val amount: Double,
    ) : SeedEvent
}

/** 起始可用现金。 */
const val SEED_START_CASH = 240000.0

/** 2026 全年历史交易 + 五只标的 2025 年度分红，按时间正序。 */
val seedEvents: List<SeedEvent> = listOf(
    SeedEvent.Buy("2026-02-02", "sh601288", "农业银行", 6.75, qty = 6000),
    SeedEvent.Buy("2026-02-02", "sh601988", "中国银行", 5.41, qty = 2200),
    SeedEvent.Buy("2026-02-02", "sh601988", "中国银行", 5.45, qty = 4300),
    SeedEvent.Buy("2026-03-09", "sh601658", "邮储银行", 5.00, qty = 10000),
    SeedEvent.Dividend("2026-05-13", "sh601288", 0.13),      // 农行 6000 股
    SeedEvent.Buy("2026-07-03", "sh601288", "农业银行", 5.91, qty = 6700),
    SeedEvent.Dividend("2026-07-10", "sh601988", 0.1169),    // 中行 6500 股
    SeedEvent.Dividend("2026-07-13", "sh601658", 0.0953),    // 邮储 10000 股
    SeedEvent.Sell("2026-07-21", "sh601988", 5.97),          // 中行全卖
    SeedEvent.Sell("2026-07-28", "sh601288", 6.72, qty = 3700),
    SeedEvent.Buy("2026-07-28", "sh600015", "华夏银行", 7.05, amount = 50000.0),
    SeedEvent.Buy("2026-07-28", "sh601319", "中国人保", 7.42, amount = 50000.0),
    SeedEvent.Sell("2026-07-28", "sh601288", 6.80, qty = 9000), // 农行全卖（剩 9000）
    SeedEvent.Buy("2026-07-28", "sh600116", "三峡水利", 6.39, amount = 50000.0),
    SeedEvent.Buy("2026-07-28", "sh601319", "中国人保", 7.35, amount = 51000.0),
    SeedEvent.Dividend("2026-07-30", "sh600015", 0.32),      // 华夏 7000 股
    SeedEvent.Dividend("2026-07-30", "sh600116", 0.075),     // 三峡 7800 股
    SeedEvent.Sell("2026-07-30", "sh601658", 5.18),          // 邮储全卖
    SeedEvent.Buy("2026-07-30", "sh601138", "工业富联", 54.10, amount = 50000.0),
    SeedEvent.Sell("2026-07-30", "sh600116", 6.55),          // 三峡全卖（≤1月分红→补税 20%）
    SeedEvent.Deposit("2026-07-30", 50000.0),                // 银证转入 5 万
    SeedEvent.Buy("2026-07-30", "sz002998", "优彩资源", 7.37, amount = 100000.0),
    SeedEvent.Dividend("2026-07-31", "sh601138", 0.65),      // 工业富联分红（除息 7.31，8.3 到账）
    SeedEvent.Dividend("2026-08-03", "sz002998", 0.20),      // 优彩资源分红（8.3 到账）
    SeedEvent.Sell("2026-08-03", "sh600015", 7.01),          // 华夏全卖（7.28买≤1月，分红补税 20%）
    SeedEvent.Buy("2026-08-05", "sz000725", "京东方A", 5.52, qty = 9900),
    SeedEvent.Sell("2026-08-05", "sh601138", 64.22),         // 工业富联全卖（7.30买≤1月，分红补税 20%）
    SeedEvent.Buy("2026-08-05", "sz002156", "通富微电", 56.10, qty = 200),
    SeedEvent.Buy("2026-08-05", "sz000036", "华联控股", 4.14, qty = 12000),
    SeedEvent.Sell("2026-08-05", "sz002998", 7.35),          // 优彩全卖（7.30买≤1月，分红补税 20%）
    SeedEvent.Buy("2026-08-05", "sz000629", "钒钛股份", 3.17, qty = 31000),
)
