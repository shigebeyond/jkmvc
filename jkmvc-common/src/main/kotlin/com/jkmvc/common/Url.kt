package com.jkmvc.common

/**
 * url与字符串互转的工具类
 *
 * @author shijianhang
 * @create 2017-12-12 下午10:27
 **/
class Url(public var protocol: String /* 协议 */,

          public var host: String /* ip */,

          public var port: Int /* 端口 */,

          public var path: String /* 路径 */ = "",

          public var parameters: MutableMap<String, String>? = null /* 参数 */
) {

    /**
     * 解析url字符串
     * @param url
     */
    public constructor(url: String) : this("", "", -1) {
        parseUrl(url)
    }


    companion object{

        /**
         * 端口的正则
         */
        val RegexPort: String = "(:(\\d+))?"

        /**
         * 参数字符串的正则
         */
        val RegexParamStr: String = "(\\?(.+))?"

        /**
         * url的正则
         */
        val RegexUrl: Regex = ("(\\w+)://([^:/]+)$RegexPort([^?]*)$RegexParamStr").toRegex()

        /**
         * 函数参数的正则
         */
        val RegexParam: Regex = "([^=]+)=([^&]+)".toRegex()
    }

    /**
     * 解析url
     *
     * @param url
     */
    protected fun parseUrl(url: String) {
        val match = RegexUrl.find(url)
        if (match == null)
            throw Exception("url格式错误: $url")
        // 协议
        protocol = match.groups[1]!!.value
        // ip
        host = match.groups[2]!!.value
        // 端口
        val portStr = match.groups[4]?.value
        if (portStr != null)
            port = portStr.toInt()
        // 路径
        path = match.groups[5]!!.value
        // 解析参数
        val paramStr = match.groups[7]?.value
        if (paramStr != null)
            parseParams(paramStr)
    }

    /**
     * 解析参数
     *
     * @param paramStr
     */
    protected fun parseParams(paramStr: String): Unit {
        val params = HashMap<String, String>()
        val matches = RegexParam.findAll(paramStr)
        for(m in matches){
            val key = m.groups[1]!!.value
            val value = m.groups[2]!!.value
            params[key] = value
        }
        this.parameters = params
    }

    /**
     * 转为字符串
     *
     * @return
     */
    public override fun toString(): String {
        // url
        val str = StringBuilder(protocol).append("://").append(host)
        if(port >= 0)
            str.append(':').append(port)
        str.append(path)
        // 参数
        if(parameters != null){
            parameters!!.entries.joinTo(str, "&", "?"){
                it.key + '=' + it.value
            }
        }
        return str.toString()
    }
}