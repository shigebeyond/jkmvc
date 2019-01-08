package com.jkmvc.validator

/**
 * 校验异常
 */
class ValidateException : RuntimeException {
    public constructor(message: String) : super(message) {
    }

    public constructor(cause: Throwable) : super(cause) {
    }

    public constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}