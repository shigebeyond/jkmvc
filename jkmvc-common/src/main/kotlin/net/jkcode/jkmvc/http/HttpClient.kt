package net.jkcode.jkmvc.http

import io.netty.handler.codec.http.cookie.Cookie
import io.netty.handler.ssl.SslContextBuilder
import net.jkcode.jkmvc.common.getAccessibleMethod
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.DefaultAsyncHttpClient
import org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder
import org.asynchttpclient.Response
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap
import io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE as InsecureTrustManager

/**
 * 使用 asynchttpclient 实现用http通讯的rpc客户端
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2019-10-19 12:48 PM
 */
class HttpClient(
        public val endpoint: String, // 服务端地址
        insecure: Boolean = false, // 不安全
        public val useCookies: Boolean = true, // 使用cookie
        public val headers: Map<String, List<String>> = emptyMap(), // 请求头
        password: String? = null, // 认证的密码
        user: String? = null // 认证的用户名
) {
    companion object{

        /**
         * DefaultAsyncHttpClient::requestBuilder()方法
         */
        protected val requestBuilderMethod = DefaultAsyncHttpClient::class.java.getAccessibleMethod("requestBuilder", String::class.java, String::class.java)
    }

    /**
     * 认证信息
     */
    protected val authorization: String? =
        if (user != null)
            Base64.getEncoder().encodeToString("$user:$password".toByteArray(UTF_8)) // base编码用户名密码
        else
            null
    /**
     * http client
     */
    protected val client = DefaultAsyncHttpClient(Builder()
        .setConnectTimeout(5000) // 连接超时
        .setSslContext(
            if (insecure) SslContextBuilder.forClient().trustManager(InsecureTrustManager).build()
            else SslContextBuilder.forClient().build()
        )
        .build()
    )

    /**
     * 记录cookie
     */
    protected val cookies: MutableMap<String, Cookie> = HashMap()

    /**
     * 发送请求
     *
     * @param method
     * @param uri
     * @param body
     * @param contentType
     * @param requestTimeout 请求超时
     * @return
     */
    protected fun send(method: String, uri: String, body: Any? = null, contentType: ContentType = ContentType.APPLICATION_FORM_URLENCODED, requestTimeout: Int = 5000): CompletableFuture<Response> {
        // 1 准备http请求
        val url = endpoint + uri
        //val req = client.preparePost(url)
        val req = requestBuilderMethod.invoke(client, method, url) as BoundRequestBuilder
        req.setCharset(Charset.defaultCharset())

        // 2 设置header
        if(contentType != null)
            req.addHeader("Content-Type", contentType)
        if (authorization != null)
            req.addHeader("Authorization", "Basic $authorization")
        headers.forEach {
            req.addHeader(it.key, it.value)
        }
        // 3 设置cookie
        if (useCookies)
            cookies.forEach {
                req.addCookie(it.value)
            }

        // 4 设置body
        contentType.setRequestBody(req, body)

        // 5 设置请求超时
        req.setRequestTimeout(requestTimeout)

        // 6 发送请求, 并返回异步响应
        return req.execute()
                .toCompletableFuture()
                .thenApply { response: Response ->
                    // 7 写cookie
                    if (useCookies) {
                        response.cookies.forEach {
                            if (it.value() == "")
                                cookies.remove(it.name())
                            else
                                cookies[it.name()] = it
                        }
                    }

                    response
                }
    }

    /**
     * 发送get请求
     *
     * @param uri
     * @param body
     * @param contentType
     * @param requestTimeout 请求超时
     * @return
     */
    public fun get(uri: String, body: Any? = null, contentType: ContentType = ContentType.APPLICATION_FORM_URLENCODED, requestTimeout: Int = 5000): CompletableFuture<Response> {
        return send("GET", uri, body, contentType, requestTimeout)
    }

    /**
     * 发送get请求
     *
     * @param uri
     * @param body
     * @param contentType
     * @param requestTimeout 请求超时
     * @return
     */
    public fun head(uri: String, body: Any? = null, contentType: ContentType = ContentType.APPLICATION_FORM_URLENCODED, requestTimeout: Int = 5000): CompletableFuture<Response> {
        return send("HEAD", uri, body, contentType, requestTimeout)
    }
    /**
     * 发送get请求
     *
     * @param uri
     * @param body
     * @param contentType
     * @param requestTimeout 请求超时
     * @return
     */
    public fun post(uri: String, body: Any? = null, contentType: ContentType = ContentType.APPLICATION_FORM_URLENCODED, requestTimeout: Int = 5000): CompletableFuture<Response> {
        return send("POST", uri, body, contentType, requestTimeout)
    }
    /**
     * 发送get请求
     *
     * @param uri
     * @param body
     * @param contentType
     * @param requestTimeout 请求超时
     * @return
     */
    public fun put(uri: String, body: Any? = null, contentType: ContentType = ContentType.APPLICATION_FORM_URLENCODED, requestTimeout: Int = 5000): CompletableFuture<Response> {
        return send("PUT", uri, body, contentType, requestTimeout)
    }
    /**
     * 发送get请求
     *
     * @param uri
     * @param body
     * @param contentType
     * @param requestTimeout 请求超时
     * @return
     */
    public fun delete(uri: String, body: Any? = null, contentType: ContentType = ContentType.APPLICATION_FORM_URLENCODED, requestTimeout: Int = 5000): CompletableFuture<Response> {
        return send("DELETE", uri, body, contentType, requestTimeout)
    }
    /**
     * 发送get请求
     *
     * @param uri
     * @param body
     * @param contentType
     * @param requestTimeout 请求超时
     * @return
     */
    public fun trace(uri: String, body: Any? = null, contentType: ContentType = ContentType.APPLICATION_FORM_URLENCODED, requestTimeout: Int = 5000): CompletableFuture<Response> {
        return send("TRACE", uri, body, contentType, requestTimeout)
    }
    /**
     * 发送get请求
     *
     * @param uri
     * @param body
     * @param contentType
     * @param requestTimeout 请求超时
     * @return
     */
    public fun options(uri: String, body: Any? = null, contentType: ContentType = ContentType.APPLICATION_FORM_URLENCODED, requestTimeout: Int = 5000): CompletableFuture<Response> {
        return send("OPTIONS", uri, body, contentType, requestTimeout)
    }
    /**
     * 发送get请求
     *
     * @param uri
     * @param body
     * @param contentType
     * @param requestTimeout 请求超时
     * @return
     */
    public fun patch(uri: String, body: Any? = null, contentType: ContentType = ContentType.APPLICATION_FORM_URLENCODED, requestTimeout: Int = 5000): CompletableFuture<Response> {
        return send("PATCH", uri, body, contentType, requestTimeout)
    }
}

