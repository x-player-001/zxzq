package com.demo.zxzq.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.zxzq.R
import com.demo.zxzq.ui.screens.ClosedPositionScreen
import com.demo.zxzq.ui.screens.ImageScreen
import com.demo.zxzq.ui.screens.LoginScreen
import com.demo.zxzq.ui.screens.MineScreen
import com.demo.zxzq.ui.screens.PlaceholderScreen
import com.demo.zxzq.ui.screens.StatementScreen
import com.demo.zxzq.ui.screens.TradeHomeScreen
import com.demo.zxzq.ui.screens.TradeQueryScreen
import com.demo.zxzq.ui.screens.TradeScreen
import com.demo.zxzq.ui.theme.ZxColors

private data class TabItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector = icon)

private val tabs = listOf(
    TabItem("首页", Icons.Outlined.Home),
    TabItem("行情", Icons.Outlined.ShowChart),
    TabItem("自选", Icons.Outlined.AddCircleOutline),
    TabItem("交易", Icons.Outlined.SwapHoriz),
    TabItem("理财", Icons.Outlined.Paid),
    TabItem("我的", Icons.Outlined.PersonOutline, selectedIcon = Icons.Filled.Person),
)

@Composable
fun AppScaffold() {
    // 默认落到「我的」页，方便直接看到主要还原页面。
    var selected by remember { mutableIntStateOf(5) }
    // 登录状态：控制「我的」页显示 未登录态 / 已登录态。默认已登录，直接看已登录界面。
    var isLoggedIn by remember { mutableStateOf(true) }
    // 登录页 / 持仓页作为全屏覆盖子页。
    var showLogin by remember { mutableStateOf(false) }
    var showTrade by remember { mutableStateOf(false) }
    // 交易主页 → 查询页 → 交割单，逐级覆盖。
    var showQuery by remember { mutableStateOf(false) }
    var showStatement by remember { mutableStateOf(false) }
    var showClosed by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(ZxColors.ScreenBg)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (selected) {
                    0 -> ImageScreen(R.drawable.home)
                    1 -> ImageScreen(R.drawable.market)
                    2 -> ImageScreen(R.drawable.watchlist)
                    3 -> TradeHomeScreen(isLoggedIn = isLoggedIn, onOpenQuery = { showQuery = true })
                    4 -> ImageScreen(R.drawable.finance)
                    5 -> MineScreen(
                        isLoggedIn = isLoggedIn,
                        onLogin = { showLogin = true },
                        onLogout = { isLoggedIn = false },
                        onOpenHolding = { showTrade = true },
                    )
                    else -> PlaceholderScreen(tabs[selected].label)
                }
            }
            BottomBar(selected = selected, onSelect = { selected = it })
        }

        // 登录页覆盖。
        if (showLogin) {
            BackHandler { showLogin = false }
            LoginScreen(
                onBack = { showLogin = false },
                onLoginSuccess = {
                    isLoggedIn = true
                    showLogin = false
                }
            )
        }

        // 全屏覆盖的持仓页（含系统返回处理）。
        if (showTrade) {
            BackHandler { showTrade = false }
            TradeScreen(
                onBack = { showTrade = false },
                onOpenStatement = { showStatement = true },
                onOpenClosed = { showClosed = true }
            )
        }

        // 查询页覆盖（交易主页“查”进入）。
        if (showQuery) {
            BackHandler { showQuery = false }
            TradeQueryScreen(
                onBack = { showQuery = false },
                onOpenStatement = { showStatement = true }
            )
        }

        // 交割单覆盖（查询页“交割单”进入）。
        if (showStatement) {
            BackHandler { showStatement = false }
            StatementScreen(onBack = { showStatement = false })
        }

        // 已清仓证券覆盖（持仓页“已清仓证券”进入）。
        if (showClosed) {
            BackHandler { showClosed = false }
            ClosedPositionScreen(onBack = { showClosed = false })
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(ZxColors.Divider))
        Row(
            Modifier
                .fillMaxWidth()
                .background(ZxColors.CardBg)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSel = index == selected
                val tint = if (isSel) ZxColors.Brand else ZxColors.TextSecondary
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(index) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isSel) tab.selectedIcon else tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tab.label,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}
