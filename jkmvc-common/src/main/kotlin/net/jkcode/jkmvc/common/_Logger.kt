package net.jkcode.jkmvc.common

import org.slf4j.LoggerFactory

internal val switcher = ModuleLogSwitcher("common")

// 公用的日志
val commonLogger = switcher.getLogger("net.jkcode")
// db的日志
val dbLogger = LoggerFactory.getLogger("net.jkcode.jkmvc.db")
// http的日志
val httpLogger = switcher.getLogger("net.jkcode.jkmvc.http")