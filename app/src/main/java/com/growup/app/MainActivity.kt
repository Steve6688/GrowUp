package com.growup.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

private val Green = Color(0xFF176B55)
private val Ink = Color(0xFF16231F)
private val Mint = Color(0xFFE4F3EC)
private val Red = Color(0xFFB3261E)

enum class AccountKind(val title: String, val isLiability: Boolean) {
    CASH("现金", false), BANK("银行卡", false), WALLET("电子钱包", false), INVESTMENT("投资", false), LIABILITY("负债", true)
}
enum class TxKind(val title: String) { INCOME("收入"), EXPENSE("支出"), TRANSFER("转账") }
data class Account(val id: String = UUID.randomUUID().toString(), val name: String, val kind: AccountKind, val balance: Double, val updatedAt: LocalDate = LocalDate.now())
data class Transaction(val id: String = UUID.randomUUID().toString(), val title: String, val kind: TxKind, val amount: Double, val category: String, val date: LocalDate = LocalDate.now())
data class Goal(val name: String, val target: Double, val current: Double, val due: LocalDate)
data class Advice(val icon: ImageVector, val title: String, val reason: String, val action: String)

/** Future connectors may read balances and transactions only. OAuth credentials stay with the provider. */
interface AccountConnector { suspend fun readAccounts(): List<Account>; suspend fun readTransactions(): List<Transaction> }

class FinanceViewModel {
    var accounts by mutableStateOf(listOf(Account(name = "日常银行卡", kind = AccountKind.BANK, balance = 12850.0), Account(name = "现金", kind = AccountKind.CASH, balance = 680.0), Account(name = "信用卡", kind = AccountKind.LIABILITY, balance = 2400.0)))
        private set
    var transactions by mutableStateOf(listOf(Transaction(title = "10 月工资", kind = TxKind.INCOME, amount = 12000.0, category = "工资", date = LocalDate.now().withDayOfMonth(5)), Transaction(title = "超市采购", kind = TxKind.EXPENSE, amount = 286.0, category = "餐饮", date = LocalDate.now().minusDays(1)), Transaction(title = "地铁出行", kind = TxKind.EXPENSE, amount = 12.0, category = "交通", date = LocalDate.now())))
        private set
    var goal by mutableStateOf(Goal("应急备用金", 30000.0, 12000.0, LocalDate.now().plusMonths(12)))
        private set

    val assets get() = accounts.filter { !it.kind.isLiability }.sumOf { it.balance }
    val liabilities get() = accounts.filter { it.kind.isLiability }.sumOf { it.balance }
    val netWorth get() = assets - liabilities
    private val monthTx get() = transactions.filter { YearMonth.from(it.date) == YearMonth.now() }
    val income get() = monthTx.filter { it.kind == TxKind.INCOME }.sumOf { it.amount }
    val expense get() = monthTx.filter { it.kind == TxKind.EXPENSE }.sumOf { it.amount }
    val surplus get() = income - expense
    val monthlyGoal: Double get() { val months = maxOf(1, ChronoUnit.MONTHS.between(YearMonth.now(), YearMonth.from(goal.due)).toInt()); return maxOf(0.0, (goal.target - goal.current) / months) }
    val advice get() = buildList {
        if (expense > income * .7) add(Advice(Icons.Default.WarningAmber, "本月支出偏高", "支出已达到本月收入的 ${"%.0f".format(expense / income * 100)}%", "本周将非必要消费控制在 ¥300 内"))
        if (surplus < monthlyGoal) add(Advice(Icons.Default.Savings, "目标储蓄落后", "每月需存 ¥${money(monthlyGoal)}，目前结余 ¥${money(surplus)}", "下次收入到账后先转入 ¥${money(monthlyGoal)}"))
        add(Advice(Icons.Default.AccountBalanceWallet, "数据来自手动记录", "账户最后更新于今天", "补记最近一笔消费，让建议更准确"))
    }.take(3)
    fun addAccount(name: String, kind: AccountKind, balance: Double) { accounts = accounts + Account(name = name, kind = kind, balance = balance) }
    fun addTransaction(title: String, kind: TxKind, amount: Double, category: String) { transactions = listOf(Transaction(title = title, kind = kind, amount = amount, category = category)) + transactions }
    fun updateGoal(name: String, target: Double, current: Double, due: LocalDate) { goal = Goal(name, target, current, due) }
}

class MainActivity : ComponentActivity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { GrowUpApp() } } }

@Composable fun GrowUpApp() {
    val finance = remember { FinanceViewModel() }; var tab by rememberSaveable { mutableIntStateOf(0) }; var showAccount by remember { mutableStateOf(false) }; var showTx by remember { mutableStateOf(false) }; var showGoal by remember { mutableStateOf(false) }
    val labels = listOf("首页", "记账", "资产", "趋势", "建议"); val icons = listOf(Icons.Default.Home, Icons.Default.AddCircle, Icons.Default.AccountBalanceWallet, Icons.Default.ShowChart, Icons.Default.Lightbulb)
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Green, background = Color(0xFFF7FAF8), surface = Color.White)) {
        Scaffold(bottomBar = { NavigationBar { labels.forEachIndexed { i, label -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(icons[i], null) }, label = { Text(label) }) } } }, floatingActionButton = { if (tab == 1) FloatingActionButton(onClick = { showTx = true }, containerColor = Green) { Icon(Icons.Default.Add, "新增记录", tint = Color.White) } }) { padding ->
            Box(Modifier.padding(padding)) { when (tab) { 0 -> Home(finance, { showTx = true }, { showAccount = true }, { showGoal = true }); 1 -> Ledger(finance); 2 -> Assets(finance, { showAccount = true }); 3 -> Trends(finance); else -> AdviceList(finance) } }
        }
        if (showAccount) AccountDialog({ showAccount = false }) { n, k, b -> finance.addAccount(n, k, b); showAccount = false }
        if (showTx) TransactionDialog({ showTx = false }) { n, k, a, c -> finance.addTransaction(n, k, a, c); showTx = false }
        if (showGoal) GoalDialog(finance.goal, { showGoal = false }) { n, t, c, d -> finance.updateGoal(n, t, c, d); showGoal = false }
    }
}

@Composable fun Home(f: FinanceViewModel, addTx: () -> Unit, addAccount: () -> Unit, editGoal: () -> Unit) = Page("早上好") {
    Text("这是你的财务概览", color = Color.Gray); Spacer(Modifier.height(16.dp)); NetWorth(f); Spacer(Modifier.height(16.dp)); Section("本月现金流") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Metric("收入", f.income, Green); Metric("支出", f.expense, Red); Metric("结余", f.surplus, Ink) } }; Spacer(Modifier.height(16.dp)); Section("今日建议", action = "查看全部") { f.advice.take(2).forEach { AdviceCard(it) } }; Spacer(Modifier.height(16.dp)); GoalCard(f.goal, f.monthlyGoal, editGoal); Spacer(Modifier.height(16.dp)); Text("最近记录", fontWeight = FontWeight.Bold); f.transactions.take(3).forEach { TransactionRow(it) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick = addAccount, modifier = Modifier.weight(1f)) { Text("添加账户") }; Button(onClick = addTx, modifier = Modifier.weight(1f)) { Text("记一笔") } }
}
@Composable fun Ledger(f: FinanceViewModel) = Page("记账") { Text("所有记录", color = Color.Gray); Spacer(Modifier.height(12.dp)); if (f.transactions.isEmpty()) Empty("还没有记录", "点击右下角，记录第一笔收支") else f.transactions.forEach { TransactionRow(it) } }
@Composable fun Assets(f: FinanceViewModel, add: () -> Unit) = Page("资产") { NetWorth(f); Spacer(Modifier.height(18.dp)); Text("资产账户", fontWeight = FontWeight.Bold); f.accounts.filter { !it.kind.isLiability }.forEach { AccountRow(it) }; Spacer(Modifier.height(12.dp)); Text("负债", fontWeight = FontWeight.Bold); f.accounts.filter { it.kind.isLiability }.forEach { AccountRow(it) }; Spacer(Modifier.height(16.dp)); OutlinedButton(onClick = add, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("手动添加账户") }; Text("账户连接仅支持只读授权；不保存银行密码。", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 12.dp)) }
@Composable fun Trends(f: FinanceViewModel) = Page("趋势") { Section("本月总结") { Text("本月收入 ¥${money(f.income)}，支出 ¥${money(f.expense)}，结余 ¥${money(f.surplus)}。", lineHeight = 24.sp); Text("净资产为 ¥${money(f.netWorth)}。持续记录可形成更完整的趋势。", color = Color.Gray, modifier = Modifier.padding(top = 8.dp)) }; Spacer(Modifier.height(16.dp)); Section("支出分类") { f.transactions.filter { it.kind == TxKind.EXPENSE }.groupBy { it.category }.forEach { (c, tx) -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(c); Text("¥${money(tx.sumOf { it.amount })}", fontWeight = FontWeight.Bold) } } } }
@Composable fun AdviceList(f: FinanceViewModel) = Page("建议") { Text("依据你的已记录数据生成，每日最多 3 条。", color = Color.Gray); Spacer(Modifier.height(12.dp)); f.advice.forEach { AdviceCard(it) } }

@Composable fun Page(title: String, content: @Composable ColumnScope.() -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 24.dp)) { item { Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(4.dp)); Column(content = content) } } }
@Composable fun NetWorth(f: FinanceViewModel) { Card(colors = CardDefaults.cardColors(containerColor = Green), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(22.dp)) { Text("总净资产", color = Color.White.copy(.8f)); Text("¥${money(f.netWorth)}", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("资产 ¥${money(f.assets)}", color = Color.White); Text("负债 ¥${money(f.liabilities)}", color = Color.White) } } } }
@Composable fun Section(title: String, action: String? = null, content: @Composable ColumnScope.() -> Unit) { Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.Bold); action?.let { Text(it, color = Green, fontSize = 13.sp) } }; Spacer(Modifier.height(10.dp)); content() } } }
@Composable fun Metric(label: String, value: Double, color: Color) { Column { Text(label, color = Color.Gray, fontSize = 13.sp); Text("¥${money(value)}", color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp) } }
@Composable fun AdviceCard(a: Advice) { Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = Mint), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) { Icon(a.icon, null, tint = Green); Spacer(Modifier.width(10.dp)); Column { Text(a.title, fontWeight = FontWeight.Bold); Text(a.reason, fontSize = 13.sp, color = Color.DarkGray); Text(a.action, fontSize = 13.sp, color = Green, modifier = Modifier.padding(top = 4.dp)) } } } }
@Composable fun GoalCard(g: Goal, monthly: Double, edit: () -> Unit) { Section("储蓄目标", "编辑") { Text(g.name, fontWeight = FontWeight.Bold); LinearProgressIndicator(progress = { (g.current / g.target).toFloat() }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), color = Green); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("¥${money(g.current)} / ¥${money(g.target)}", fontSize = 13.sp); Text("每月建议 ¥${money(monthly)}", fontSize = 13.sp, color = Green) }; Text("目标日期 ${g.due}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp)); TextButton(onClick = edit, contentPadding = PaddingValues(0.dp)) { Text("调整目标") } } }
@Composable fun TransactionRow(t: Transaction) { Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = RoundedCornerShape(12.dp), color = if (t.kind == TxKind.INCOME) Mint else Color(0xFFF3F1EE), modifier = Modifier.size(42.dp)) { Icon(if (t.kind == TxKind.INCOME) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = if (t.kind == TxKind.INCOME) Green else Red, modifier = Modifier.padding(10.dp)) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(t.title, fontWeight = FontWeight.Medium); Text("${t.category} · ${t.date}", fontSize = 12.sp, color = Color.Gray) }; Text((if (t.kind == TxKind.INCOME) "+" else "-") + "¥${money(t.amount)}", color = if (t.kind == TxKind.INCOME) Green else Ink, fontWeight = FontWeight.Bold) } }
@Composable fun AccountRow(a: Account) { Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(a.name, fontWeight = FontWeight.Medium); Text("${a.kind.title} · 更新于 ${a.updatedAt}", fontSize = 12.sp, color = Color.Gray) }; Text("¥${money(a.balance)}", fontWeight = FontWeight.Bold, color = if (a.kind.isLiability) Red else Ink) } }
@Composable fun Empty(title: String, hint: String) { Column(Modifier.fillMaxWidth().padding(top = 88.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(52.dp), tint = Green); Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)); Text(hint, color = Color.Gray, fontSize = 13.sp) } }

@Composable fun AccountDialog(close: () -> Unit, save: (String, AccountKind, Double) -> Unit) { var name by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var kind by remember { mutableStateOf(AccountKind.BANK) }; AlertDialog(onDismissRequest = close, title = { Text("添加账户") }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("账户名称") }); AccountKind.entries.forEach { Row(Modifier.fillMaxWidth().clickable { kind = it }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(kind == it, { kind = it }); Text(it.title) } }; OutlinedTextField(amount, { amount = it }, label = { Text("当前余额") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)) } }, confirmButton = { Button(onClick = { amount.toDoubleOrNull()?.let { if (name.isNotBlank()) save(name, kind, it) } }) { Text("保存") } }, dismissButton = { TextButton(onClick = close) { Text("取消") } }) }
@Composable fun TransactionDialog(close: () -> Unit, save: (String, TxKind, Double, String) -> Unit) { var name by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var category by remember { mutableStateOf("日常") }; var kind by remember { mutableStateOf(TxKind.EXPENSE) }; AlertDialog(onDismissRequest = close, title = { Text("新增记录") }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("说明") }); Row { TxKind.entries.forEach { FilterChip(selected = kind == it, onClick = { kind = it }, label = { Text(it.title) }, modifier = Modifier.padding(end = 4.dp)) } }; OutlinedTextField(amount, { amount = it }, label = { Text("金额") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)); OutlinedTextField(category, { category = it }, label = { Text("分类") }) } }, confirmButton = { Button(onClick = { amount.toDoubleOrNull()?.let { if (name.isNotBlank()) save(name, kind, it, category) } }) { Text("保存") } }, dismissButton = { TextButton(onClick = close) { Text("取消") } }) }
@Composable fun GoalDialog(g: Goal, close: () -> Unit, save: (String, Double, Double, LocalDate) -> Unit) { var name by remember { mutableStateOf(g.name) }; var target by remember { mutableStateOf(g.target.toString()) }; var current by remember { mutableStateOf(g.current.toString()) }; AlertDialog(onDismissRequest = close, title = { Text("设置储蓄目标") }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("目标名称") }); OutlinedTextField(target, { target = it }, label = { Text("目标金额") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)); OutlinedTextField(current, { current = it }, label = { Text("当前已存") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)); Text("目标日期暂设为 ${g.due}", fontSize = 12.sp, color = Color.Gray) } }, confirmButton = { Button(onClick = { val t = target.toDoubleOrNull(); val c = current.toDoubleOrNull(); if (t != null && c != null && name.isNotBlank()) save(name, t, c, g.due) }) { Text("保存") } }, dismissButton = { TextButton(onClick = close) { Text("取消") } }) }
fun money(value: Double) = "%,.0f".format(value)
