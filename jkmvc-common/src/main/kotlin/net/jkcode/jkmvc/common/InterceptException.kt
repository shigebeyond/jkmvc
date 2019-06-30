package net.jkcode.jkmvc.common

import net.jkcode.jkmvc.common.JkException

/**
 * 拦截器异常
 */
class InterceptException : JkException {
    public constructor(message: String) : super(message) {
    }

    public constructor(cause: Throwable) : super(cause) {
    }

    public constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}