package com.jkmvc.common

/**
 * 服务url
 *
 * @author shijianhang
 * @create 2017-12-12 下午10:27
 **/
class URL( public var protocol: String /* 协议 */,

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
        // 解析url
        val match = RegexUrl.find(url)
        if(match == null)
            throw Exception("url格式错误: $url")
        protocol = match.groups[1]!!.value // 协议
        host = match.groups[2]!!.value // ip
        // 端口
        val portStr = match.groups[4]?.value
        if(portStr != null)
            port = portStr.toInt()
        path = match.groups[5]!!.value // 路径

        // 解析参数
        val paramStr = match.groups[7]?.value
        if(paramStr != null)
            parameters = parseParams(paramStr)
    }

    companion object{

        /**
         * url的正则
         */
        val RegexUrl: Regex = "(\\w+)://([^:/]+)(:(\\d+))?([^?]*)(\\?(.+))?".toRegex()

        /**
         * 函数参数的正则
         */
        val RegexParam: Regex = "([^=]+)=([^&]+)".toRegex()

        /**
         * 解析参数
         *
         * @param paramStr
         * @return
         */
        public fun parseParams(paramStr: String): HashMap<String, String> {
            val params = HashMap<String, String>()
            val matches = RegexParam.findAll(paramStr)
            for(m in matches){
                val key = m.groups[1]!!.value
                val value = m.groups[2]!!.value
                params[key] = value
            }
            return params
        }
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