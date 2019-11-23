package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.JkException

/**
 * db异常
 */
class DbException : JkException {
    public constructor(message: String) : super(message) {
    }

    public constructor(cause: Throwable) : super(cause) {
    }

    public constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}