package com.demo.zxzq.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 交易数据单例仓库 + 交易引擎：持仓 / 可用资金 / 委托 / 交割单流水，以 StateFlow 暴露供 Compose 观察。
 *
 * 税费（佣金/印花税/过户费/红利税）统一由引擎结算（见 FeeConfig）。买卖成本价含佣金摊入。
 * 首次启动（DataStore 为空）用 SeedTrades 逐笔重放生成全部状态。数据经 TradeStore 持久化。
 */
object TradeRepository {

    private val _state = MutableStateFlow(TradeState())
    val state: StateFlow<TradeState> = _state.asStateFlow()

    private var store: TradeStore? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var seq = 0L

    // UI 手动操作日志（成交/分红/入金），可撤销最后一笔。历史 seedEvents 不在此列。
    // 可撤销数量通过 TradeState.uiOpCount 暴露给 UI。
    private val uiOps = mutableListOf<SeedEvent>()

    // 最近一次行情快照，仅供卖出时抓昨收记进事件（不参与重放确定性）。
    @Volatile private var lastQuotes: Map<String, Quote> = emptyMap()

    /** 三屏拉到行情后调用，缓存供卖出时抓昨收。 */
    fun updateQuotes(q: Map<String, Quote>) {
        if (q.isNotEmpty()) lastQuotes = q
    }

    fun init(context: Context) {
        if (store != null) return
        val s = TradeStore(context)
        store = s
        scope.launch {
            // 优先服务器（多设备权威，重装恢复的来源）；离线/首次回退本地 DataStore。
            val remote = SyncApi.pull()
            val ops = remote ?: s.load()
            if (ops != null) {
                uiOps.clear(); uiOps.addAll(ops)
            }
            _state.value = replayAll()
            // 命中远程才把服务器数据回写本地（含新机远程为空→清本地）。
            // 未命中远程（离线）时不回写，避免用空/本地数据覆盖服务器。
            if (remote != null) s.save(uiOps.toList())
        }
    }

    /** 重放一个种子/UI 事件到状态上（seed 重放与 UI 操作重放共用）。 */
    private fun applyEvent(st: TradeState, e: SeedEvent): TradeState = when (e) {
        is SeedEvent.Buy -> {
            val qty = e.qty ?: lotsForAmount(e.amount ?: 0.0, e.price)
            applyBuy(st, e.code, e.name, e.price, qty, e.date).first
        }
        is SeedEvent.Sell -> {
            val held = st.holdings.find { it.code == e.code }?.shares ?: 0L
            val qty = e.qty ?: held
            val next = applySell(st, e.code, e.price, qty, e.date).first
            // 今日卖出（且有昨收）→ 记进 todaySells 供当日盈亏；跨日 date≠today 自动不计
            if (e.date == today() && e.prevClose != null && e.prevClose > 0.0) {
                next.copy(todaySells = next.todaySells + TodaySell(e.code, e.price, e.prevClose, qty))
            } else next
        }
        is SeedEvent.Dividend -> applyDividend(st, e.code, e.perShare, e.date).first
        is SeedEvent.Deposit -> applyDeposit(st, e.amount, e.date)
    }

    /** 重放 历史种子 + UI 操作日志，得出完整状态（带上可撤销操作数）。 */
    private fun replayAll(): TradeState =
        (seedEvents + uiOps).fold(TradeState(cash = SEED_START_CASH)) { st, e -> applyEvent(st, e) }
            .copy(uiOpCount = uiOps.size)

    private fun persist() {
        val s = store ?: return
        val snapshot = uiOps.toList()   // 只持久化 UI 操作日志，状态由 seed+uiOps 重建
        scope.launch { s.save(snapshot) }        // 本地：永远先写（离线兜底）
        scope.launch { SyncApi.push(snapshot) }  // 服务器：整份覆盖，失败静默
    }

    private fun nextId(): String = "${System.currentTimeMillis()}-${seq++}"

    /** 按金额买入 → 整百股向下取。 */
    private fun lotsForAmount(amount: Double, price: Double): Long =
        if (price <= 0.0) 0L else ((amount / price).toLong() / 100L) * 100L

    /* ============================ 对外业务操作 ============================ */

    /** 下单：校验 A 股规则后生成一条「已报」委托（不动资金）。 */
    fun placeOrder(side: OrderSide, code: String, name: String, price: Double, qty: Long): OrderResult {
        if (code.isBlank()) return OrderResult.Failure("请选择证券")
        if (price <= 0.0) return OrderResult.Failure("价格必须大于 0")
        if (qty <= 0L) return OrderResult.Failure("数量必须大于 0")
        if (qty % 100 != 0L) return OrderResult.Failure("数量必须为 100 的整数倍")

        val st = _state.value
        when (side) {
            OrderSide.BUY -> {
                val cost = price * qty + FeeConfig.commission(price * qty) + FeeConfig.transferFee(price * qty)
                if (cost > st.cash + 1e-6) return OrderResult.Failure("可用资金不足")
            }
            OrderSide.SELL -> {
                val held = st.holdings.find { it.code == code }?.shares ?: 0L
                if (held == 0L) return OrderResult.Failure("无持仓可卖")
                if (qty > held) return OrderResult.Failure("可卖数量不足")
            }
        }

        val order = Order(
            id = nextId(), code = code, name = name, side = side,
            price = price, qty = qty, status = OrderStatus.REPORTED,
            time = System.currentTimeMillis(),
        )
        _state.update { it.copy(orders = it.orders + order) }
        persist()
        return OrderResult.Success
    }

    /** 成交：已报 → 已成，走引擎结算（含税费）。追加一条 UI 操作日志，可撤销。 */
    fun fillOrder(orderId: String): OrderResult {
        val st = _state.value
        val o = st.orders.find { it.id == orderId && it.status == OrderStatus.REPORTED }
            ?: return OrderResult.Failure("委托不存在或已处理")

        // 先试算校验（不落地），失败则不追加日志。
        val (_, result) = when (o.side) {
            OrderSide.BUY -> applyBuy(st, o.code, o.name, o.price, o.qty, today())
            OrderSide.SELL -> applySell(st, o.code, o.price, o.qty, today())
        }
        if (result is OrderResult.Failure) return result

        val ev: SeedEvent = when (o.side) {
            OrderSide.BUY -> SeedEvent.Buy(today(), o.code, o.name, o.price, qty = o.qty)
            OrderSide.SELL -> SeedEvent.Sell(
                today(), o.code, o.price, qty = o.qty,
                prevClose = lastQuotes[o.code]?.prevClose,  // 抓当前昨收记死，算当日盈亏用
            )
        }
        uiOps.add(ev)
        // 委托标 FILLED（orders 不参与重放，重放后接回）。
        val filledOrders = st.orders.map { x -> if (x.id == orderId) x.copy(status = OrderStatus.FILLED) else x }
        _state.value = replayAll().copy(orders = filledOrders)
        persist()
        return OrderResult.Success
    }

    /** 分红入账（不扣税，累计未完税额，卖出时补税）。code=行情代码，perShare=每股股息。 */
    fun addDividend(code: String, perShare: Double): OrderResult {
        if (perShare <= 0.0) return OrderResult.Failure("每股股息必须大于 0")
        val (_, result) = applyDividend(_state.value, code, perShare, today())
        if (result is OrderResult.Failure) return result
        uiOps.add(SeedEvent.Dividend(today(), code, perShare))
        _state.value = replayAll().copy(orders = _state.value.orders)
        persist()
        return OrderResult.Success
    }

    /** 撤单：已报 → 已撤，不影响资金/持仓。 */
    fun cancelOrder(orderId: String): OrderResult {
        val o = _state.value.orders.find { it.id == orderId && it.status == OrderStatus.REPORTED }
            ?: return OrderResult.Failure("委托不存在或已处理")
        _state.value = _state.value.copy(
            orders = _state.value.orders.map { x -> if (x.id == orderId) x.copy(status = OrderStatus.CANCELED) else x }
        )
        persist()
        return OrderResult.Success
    }

    /* ============================ 引擎结算核心（纯函数，带日期+税费） ============================ */

    /** 买入结算：佣金+过户费；成本含佣金摊入；新建仓记首次买入日。 */
    private fun applyBuy(
        st: TradeState, code: String, name: String, price: Double, qty: Long, date: String
    ): Pair<TradeState, OrderResult> {
        if (qty <= 0L) return st to OrderResult.Failure("数量必须大于 0")
        val turnover = price * qty
        val commission = FeeConfig.commission(turnover)
        val transfer = FeeConfig.transferFee(turnover)
        val outlay = turnover + commission + transfer
        if (outlay > st.cash + 1e-6) return st to OrderResult.Failure("可用资金不足")

        val newCash = st.cash - outlay
        val isToday = date == today()
        val boughtToday = if (isToday) qty else 0L          // T+1：当天买入计入，跨日重放自动为 0
        val buyTodayCost = if (isToday) turnover else 0.0   // 今买纯成交额（不含费），算当日盈亏用
        val ex = st.holdings.find { it.code == code }
        val newHoldings: List<Holding> = if (ex == null) {
            st.holdings + Holding(
                code = code, name = name, shares = qty,
                cost = outlay / qty,          // 成本含佣金/过户费
                firstBuyDate = date, untaxedDividend = 0.0,
                boughtShares = qty, buyCost = outlay, buyTurnover = turnover,
                todayBought = boughtToday, todayBuyCost = buyTodayCost,
            )
        } else {
            st.holdings.map {
                if (it.code == code) {
                    val total = it.shares + qty
                    val newCost = (it.cost * it.shares + outlay) / total  // 加权平均（含费）
                    it.copy(
                        shares = total, cost = newCost, // 首次买入日/未完税分红保留
                        boughtShares = it.boughtShares + qty,
                        buyCost = it.buyCost + outlay,
                        buyTurnover = it.buyTurnover + turnover,
                        todayBought = it.todayBought + boughtToday,  // 同日多次买累加
                        todayBuyCost = it.todayBuyCost + buyTodayCost,
                    )
                } else it
            }
        }
        val secBal = newHoldings.find { it.code == code }?.shares ?: 0L
        val stmt = statement(
            title = "证券买入", date = date, signedAmount = -outlay, balance = newCash,
            turnover = turnover, commission = commission, transfer = transfer, stampTax = 0.0,
            name = name, code = code, dealPrice = price, dealQty = qty.toDouble(), secBalance = secBal.toDouble(),
        )
        return st.copy(holdings = newHoldings, cash = newCash, statements = st.statements + stmt) to OrderResult.Success
    }

    /** 卖出结算：佣金+印花税+过户费；按持股时长对未完税分红补红利税（单列一条流水）。 */
    private fun applySell(
        st: TradeState, code: String, price: Double, qty: Long, date: String
    ): Pair<TradeState, OrderResult> {
        val ex = st.holdings.find { it.code == code } ?: return st to OrderResult.Failure("无持仓可卖")
        if (qty <= 0L) return st to OrderResult.Failure("数量必须大于 0")
        if (qty > ex.shares) return st to OrderResult.Failure("可卖数量不足")

        val turnover = price * qty
        val commission = FeeConfig.commission(turnover)
        val stamp = FeeConfig.stampTax(turnover)
        val transfer = FeeConfig.transferFee(turnover)
        val proceeds = turnover - commission - stamp - transfer

        val clearing = qty == ex.shares
        // 红利补税：全部清仓时对该股累计未完税分红补税；部分卖出不结转（留到全清）。
        val dividendTax = if (clearing) {
            ex.untaxedDividend * FeeConfig.dividendTaxRate(daysBetween(ex.firstBuyDate, date))
        } else 0.0

        val soldCost = ex.cost * qty  // 本次成本，卖出不改成本 → Σ soldCost 恰为总买入成本，无重复

        var newCash = st.cash + proceeds
        val newHoldings = st.holdings.mapNotNull {
            when {
                it.code != code -> it
                clearing -> null
                else -> it.copy(                          // 部分卖出：累积卖出所得，未完税分红保留
                    shares = it.shares - qty,
                    sellProceeds = it.sellProceeds + proceeds,
                )
            }
        }
        // 已清仓记录：同一只股同一轮的多次卖出合并到同一条（用 partial 未封口标志匹配）。
        val existingIdx = st.closed.indexOfLast { it.code == code && it.partial }
        val newClosed = if (existingIdx >= 0) {
            val old = st.closed[existingIdx]
            val merged = old.copy(
                soldShares = old.soldShares + qty,
                soldCost = old.soldCost + soldCost,
                soldTurnover = old.soldTurnover + turnover,
                proceeds = old.proceeds + proceeds,
                dividend = if (clearing) ex.dividendTotal else old.dividend,
                dividendTax = if (clearing) dividendTax else old.dividendTax,
                closeDate = date,
                partial = !clearing,   // 全清则封口
            )
            // 移到列表末尾，使"最近卖出"永远排最后（UI 倒序后即最上）。
            st.closed.toMutableList().apply { removeAt(existingIdx); add(merged) }
        } else {
            st.closed + ClosedPosition(
                code = code, name = ex.name,
                soldShares = qty,
                soldCost = soldCost,
                soldTurnover = turnover,
                proceeds = proceeds,
                dividend = if (clearing) ex.dividendTotal else 0.0,
                dividendTax = dividendTax,
                firstBuyDate = ex.firstBuyDate,
                closeDate = date,
                partial = !clearing,
            )
        }
        val secBal = newHoldings.find { it.code == code }?.shares ?: 0L
        var stmts = st.statements + statement(
            title = "证券卖出", date = date, signedAmount = proceeds, balance = newCash,
            turnover = turnover, commission = commission, transfer = transfer, stampTax = stamp,
            name = ex.name, code = code, dealPrice = price, dealQty = qty.toDouble(), secBalance = secBal.toDouble(),
        )
        // 红利税单列一条扣款流水
        if (dividendTax > 1e-6) {
            newCash -= dividendTax
            stmts = stmts + statement(
                title = "红利税", date = date, signedAmount = -dividendTax, balance = newCash,
                turnover = dividendTax, commission = 0.0, transfer = 0.0, stampTax = dividendTax,
                name = ex.name, code = code, dealPrice = 0.0, dealQty = 0.0, secBalance = secBal.toDouble(),
            )
        }
        return st.copy(holdings = newHoldings, cash = newCash, statements = stmts, closed = newClosed) to OrderResult.Success
    }

    /** 分红结算：加现金（不扣税），累计未完税额。 */
    private fun applyDividend(
        st: TradeState, code: String, perShare: Double, date: String
    ): Pair<TradeState, OrderResult> {
        val h = st.holdings.find { it.code == code } ?: return st to OrderResult.Failure("无该证券持仓")
        val amount = perShare * h.shares
        val newCash = st.cash + amount
        val newHoldings = st.holdings.map {
            if (it.code == code) it.copy(
                untaxedDividend = it.untaxedDividend + amount,
                dividendTotal = it.dividendTotal + amount,
            ) else it
        }
        val stmt = statement(
            title = "股息入帐", date = date, signedAmount = amount, balance = newCash,
            turnover = amount, commission = 0.0, transfer = 0.0, stampTax = 0.0,
            name = h.name, code = code, dealPrice = perShare, dealQty = 0.0, secBalance = h.shares.toDouble(),
            dealPriceDigits = 4,
        )
        return st.copy(holdings = newHoldings, cash = newCash, statements = st.statements + stmt) to OrderResult.Success
    }

    /** 入金结算：加现金，写一条「银证转入」流水（不涉及持仓/税费）。 */
    private fun applyDeposit(st: TradeState, amount: Double, date: String): TradeState {
        val newCash = st.cash + amount
        val stmt = statement(
            title = "银证转入", date = date, signedAmount = amount, balance = newCash,
            turnover = amount, commission = 0.0, transfer = 0.0, stampTax = 0.0,
            name = "", code = "", dealPrice = 0.0, dealQty = 0.0, secBalance = 0.0,
        )
        return st.copy(cash = newCash, statements = st.statements + stmt)
    }

    /** 对外入金入口（银证转入）。 */
    fun deposit(amount: Double): OrderResult {
        if (amount <= 0.0) return OrderResult.Failure("入金金额必须大于 0")
        uiOps.add(SeedEvent.Deposit(today(), amount))
        _state.value = replayAll().copy(orders = _state.value.orders)
        persist()
        return OrderResult.Success
    }

    /** 撤回最后一笔 UI 操作（成交/分红/入金），回到该操作之前的状态。历史种子不可撤。 */
    fun undoLastUiOp(): OrderResult {
        if (uiOps.isEmpty()) return OrderResult.Failure("无可撤销的操作")
        uiOps.removeAt(uiOps.size - 1)
        _state.value = replayAll().copy(orders = _state.value.orders)
        persist()
        return OrderResult.Success
    }

    /* ============================ 辅助 ============================ */

    private fun statement(
        title: String, date: String, signedAmount: Double, balance: Double,
        turnover: Double, commission: Double, transfer: Double, stampTax: Double,
        name: String, code: String, dealPrice: Double, dealQty: Double, secBalance: Double,
        dealPriceDigits: Int = 4,
    ): StatementItem = StatementItem(
        title = title, date = date, amount = money(signedAmount), balance = money(balance),
        turnover = money(turnover),
        fee = money(commission), stampTax = money(stampTax), transferFee = money(transfer),
        surcharge = "0", clearingFee = "0.00",
        secName = name, secCode = code.dropWhile { !it.isDigit() },
        dealPrice = String.format("%.${dealPriceDigits}f", dealPrice),
        dealQty = money(dealQty), secBalance = money(secBalance),
    )

    private fun today(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    /** 两个 yyyy-MM-dd 日期相差天数（from→to）；解析失败按 0。 */
    private fun daysBetween(from: String, to: String): Long {
        if (from.isBlank() || to.isBlank()) return 0
        return try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val a = fmt.parse(from)?.time ?: return 0
            val b = fmt.parse(to)?.time ?: return 0
            (b - a) / (1000L * 60 * 60 * 24)
        } catch (e: Exception) { 0 }
    }
}
