package net.jkcode.jkmvc.es

import net.jkcode.jkutil.common.JkException

/**
 * es操作异常
 *
 * @author shijianhang
 * @date 2016-10-21 下午5:16:59  
 *
 */
class EsException : JkException {

    public var failedDocs: Map<String, String>
        protected set


    public constructor(message: String, failedDocuments: Map<String, String> = emptyMap()) : super(message) {
        this.failedDocs = failedDocuments
    }

    public constructor(cause: Throwable, failedDocuments: Map<String, String> = emptyMap()) : super(cause) {
        this.failedDocs = failedDocuments
    }

    public constructor(message: String, cause: Throwable? = null, failedDocuments: Map<String, String> = emptyMap()) : super(message, cause) {
        this.failedDocs = failedDocuments
    }
}