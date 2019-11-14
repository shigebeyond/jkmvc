package net.jkcode.jkmvc.common

internal val switcher = ModuleLogSwitcher("common")

// 公用的日志
val commonLogger = switcher.getLogger("net.jkcode")
// db的日志
val dbLogger = switcher.getLogger("net.jkcode.jkmvc.db")
// http的日志
val httpLogger = switcher.getLogger("net.jkcode.jkmvc.http")