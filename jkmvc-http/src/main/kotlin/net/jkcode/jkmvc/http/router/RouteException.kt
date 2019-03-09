package net.jkcode.jkmvc.http.router

import net.jkcode.jkmvc.common.JkException

/**
 * 路由异常
 */
class RouteException : JkException {
    public constructor(message: String) : super(message) {
    }

    public constructor(cause: Throwable) : super(cause) {
    }

    public constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}