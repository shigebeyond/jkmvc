package net.jkcode.jkmvc.common

import java.net.NetworkInterface

/**
 * 获得内网ip
 *
 * @return
 */
public fun getIntranetHost(): String {
    for(netIntf in NetworkInterface.getNetworkInterfaces()){
        for(addr in netIntf.inetAddresses){
            val ip = addr.hostAddress
            if (ip.startsWith("192.168."))
                return ip;

        }
    }
    return "127.0.0.1"
}

// IRpcServer类
private val serverClass: Class<*>? by lazy {
    try {
        Class.forName("net.jkcode.jksoa.server.IRpcServer")
    } catch (e: ClassNotFoundException) {
        null
    }
}

// IRpcServer.current() 方法
private val currentServerMethod by lazy{
    serverClass!!.getMethod("current")
}

/**
 * 获得当前启动的 IRpcServer
 */
public fun currentServer(): Any? {
    if(serverClass == null)
        return null

    return currentServerMethod.invoke(null)
}

/**
 * 获得本地的ip+port
 * @return
 */
public fun getLocalHostPort(): Pair<String, Int> {
    // 1 没有启动server, 则取本地地址
    if(currentServer() == null)
        return Pair(getIntranetHost(), 0)

    // 2 启动server, 则取server配置的地址
    val config = Config.instance("server", "yaml")
    return Pair(config.getString("host", getIntranetHost())!!, config["port"]!!)
}