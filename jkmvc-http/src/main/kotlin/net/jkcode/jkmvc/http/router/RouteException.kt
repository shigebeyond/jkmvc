package net.jkcode.jkmvc.http.router

import net.jkcode.jkutil.common.JkException

/**
 * 路由异常
 */
class RouteException : JkException {

    public constructor(cause: Throwable) : super(cause) {
    }

    public constructor(message: String, cause: Throwable? = null) : super(message, cause) {
    }
}