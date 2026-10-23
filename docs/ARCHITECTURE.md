# 架构

Kotlin + Jetpack Compose 单 Activity MVP。`FinanceViewModel` 维护本地 UI 状态并计算净资产、月结余和目标月储蓄额。后续以 Repository + Room 替换内存数据；`AccountConnector` 仅定义读取余额和交易的边界。
