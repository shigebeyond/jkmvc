package net.jkcode.jkmvc.es

import io.searchbox.client.JestResult
import net.jkcode.jkutil.common.JkException

/**
 * es操作异常
 *
 * @author shijianhang
 * @date 2021-4-21 下午5:16:59
 *
 */
class EsException : JkException {

    /**
     * 错误的文档
     */
    public var result: JestResult?
        protected set


    public constructor(message: String, failedDocuments: JestResult? = null) : super(message) {
        this.result = failedDocuments
    }

    public constructor(cause: Throwable, failedDocuments: JestResult? = null) : super(cause) {
        this.result = failedDocuments
    }

    public constructor(message: String, cause: Throwable? = null, failedDocuments: JestResult? = null) : super(message, cause) {
        this.result = failedDocuments
    }
}