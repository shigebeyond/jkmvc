package com.jkmvc.common

import java.util.*

/**
 * ip+端口
 */
public typealias HostAndPort = Pair<String, Int>

/**
 * 解析多地址
 *
 * @author shijianhang
 * @date 2019-1-25 下午8:02:47
 */
object AddressesParser {

    /**
     * 地址的正则
     */
    private val REGEX_ADDRESS: Regex = ("(.+):(\\d+),?").toRegex();

    /**
     * 解析出多个地址
     * @param addresses 地址表达式
     * @return
     */
    fun parse(addresses: String): List<HostAndPort> {
        val matches: Sequence<MatchResult> = REGEX_ADDRESS.findAll(addresses);
        val result: LinkedList<HostAndPort> = LinkedList();
        for(m in matches) {
            val host = m.groups[1]!!.value
            val port = m.groups[2]!!.value.toInt()
            result.add(host to port)
        }
        return result
    }
}