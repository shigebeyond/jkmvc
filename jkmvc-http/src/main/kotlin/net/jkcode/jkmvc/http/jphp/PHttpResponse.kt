package net.jkcode.jkmvc.http.jphp

import net.jkcode.jkmvc.http.HttpResponse
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.lang.BaseObject
import php.runtime.reflection.ClassEntity
import java.io.IOException
import java.nio.charset.Charset

@Reflection.Name("HttpServerResponse")
@Reflection.Namespace(JkmvcHttpExtension.Companion.NS)
class PHttpResponse(env: Environment, protected val response: HttpResponse) : BaseObject(env) {

    @Reflection.Signature
    protected fun __construct() {
    }

    @Reflection.Signature
    fun write(value: Memory): PHttpResponse {
        write(value, "UTF-8")
        return this
    }

    @Reflection.Signature
    fun write(value: Memory, charset: String?): PHttpResponse {
        response.outputStream.write(value.getBinaryBytes(Charset.forName(charset)))
        return this
    }

    @Reflection.Signature
    fun status(status: Int): PHttpResponse {
        status(status, null)
        return this
    }

    @Reflection.Signature
    fun status(status: Int, @Reflection.Nullable message: String?): PHttpResponse {
        response.status = status
        if (message != null && !message.isEmpty()) {
            response.sendError(status, message)
        }
        return this
    }

    @Reflection.Signature
    fun header(name: String?, value: Memory): PHttpResponse {
        response.addHeader(name, value.toString())
        return this
    }

    @Reflection.Signature
    fun contentType(value: String?): PHttpResponse {
        response.contentType = value
        return this
    }

    @Reflection.Signature
    fun contentLength(value: Long): PHttpResponse {
        response.setContentLengthLong(value)
        return this
    }

    @Reflection.Signature
    fun redirect(value: String?): PHttpResponse {
        response.sendRedirect(value!!)
        return this
    }

    @Reflection.Signature
    fun flush(): PHttpResponse {
        response.flushBuffer()
        return this
    }
}