package net.jkcode.jkmvc.common

import org.apache.http.Consts
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import java.util.*


/**
 * http请求发送工具类
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
object Http {

    /**
     * httpclient对象
     */
    private val httpclient = HttpClients.createDefault()

    /**
     * 发送get请求
     *
     * @param url
     * @return
     */
    public fun get(url: String): String {
        // 执行get请求，并获得响应
        val httpget = HttpGet(url)
        return httpclient.execute(httpget).use{ res ->
            // 处理响应
            EntityUtils.toString(res!!.entity)
        }
    }

    /**
     * 发送post请求
     *
     * @param url
     * @param params 参数
     * @return
     */
    public fun post(url: String, params: Map<String, String>): String {
        // 执行post请求，并获得响应
        val httppost = HttpPost(url)
        httppost.entity = map2FormEntity(params) // 参数
        return httpclient.execute(httppost).use { res ->
            // 处理响应
            EntityUtils.toString(res!!.entity)
        }
    }

    /**
     * 参数转表单域
     *
     * @param map
     * @return
     */
    private fun map2FormEntity(map: Map<String, String>): UrlEncodedFormEntity {
        val params = ArrayList<NameValuePair>()
        for ((key, value) in map) {
            params.add(BasicNameValuePair(key, value))
        }
        return UrlEncodedFormEntity(params, Consts.UTF_8)
    }
}