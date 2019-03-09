package net.jkcode.jkmvc.validator

import net.jkcode.jkmvc.common.JkException

/**
 * 校验异常
 */
class ValidateException : JkException {
    public constructor(message: String) : super(message) {
    }

    public constructor(cause: Throwable) : super(cause) {
    }

    public constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}