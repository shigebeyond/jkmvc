package net.jkcode.jkmvc.http

import net.jkcode.jkmvc.common.buildQueryString
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.request.body.multipart.ByteArrayPart
import org.asynchttpclient.request.body.multipart.FilePart
import org.asynchttpclient.request.body.multipart.InputStreamPart
import org.asynchttpclient.request.body.multipart.StringPart
import java.io.File
import java.io.InputStream

/**
 * http内容类型
 * @author shijianhang<772910474@qq.com>
 * @date 2019-06-04 9:49 AM
 */
public enum class ContentType {

    TEXT_HTML{
        override fun toString(): String = "text/html"
    },

    TEXT_PLAIN{
        override fun toString(): String = "text/plain"
    },

    APPLICATION_FORM_URLENCODED{
        override fun toString(): String = "application/x-www-form-urlencoded"
    },

    MULTIPART_FORM_DATA{
        override fun toString(): String = "multipart/form-data"
    },

    APPLICATION_JSON{
        override fun toString(): String = "application/json"
    },

    APPLICATION_XML{
        override fun toString(): String = "application/xml"
    },

    TEXT_XML{
        override fun toString(): String = "text/xml"
    },

    APPLICATION_OCTET_STREAM{
        override fun toString(): String = "application/octet-stream"
    };

    /**
     * 设置请求body
     */
    public open fun setRequestBody(req: BoundRequestBuilder, body: Any?){
        if(body == null)
            return

        if(body is String){
            req.setBody(body)
            return
        }

        if(body is File){
            req.setBody(body)
            return
        }

        if(body is ByteArray){
            req.setBody(body)
            return
        }

        if(body is InputStream){
            req.setBody(body)
            return
        }

        if(body is Map<*, *>) {
            // 文件上传
            if(this == MULTIPART_FORM_DATA){
                body.forEach { key, value ->
                    val name = key.toString()
                    val part = when(value){
                        null -> StringPart(name, null)
                        is File -> FilePart(name, value)
                        is ByteArray -> ByteArrayPart(name, value)
                        is InputStream -> InputStreamPart(name, value, null)
                        else -> StringPart(name, value.toString())
                    }
                    req.addBodyPart(part)
                }
                return
            }

            // 表单提交
            req.setBody(body.buildQueryString(true))
            return
        }

        req.setBody(body.toString())
    }


}