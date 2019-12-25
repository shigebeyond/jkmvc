package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.JkException

/**
 * db异常
 */
class DbException : JkException {

    public constructor(cause: Throwable) : super(cause) {
    }

    public constructor(message: String, cause: Throwable? = null) : super(message, cause) {
    }
}