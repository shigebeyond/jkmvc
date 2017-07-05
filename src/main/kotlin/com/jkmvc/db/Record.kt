package com.jkmvc.db

import java.util.*

/**
 * 封装查询结果
 * 扩展 [] 操作符，来代理属性读写
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
class Record(protected val data: MutableMap<String, Any?> = HashMap<String, Any?>()) : IRecord {

    /**
     * 设置属性
     */
    public override operator fun set(column: String, value: Any?) {
        data.set(column, value)
    }

    /**
     * 读取属性
     */
    public override operator fun <T> get(name: String, defaultValue: Any?): T {
        return (data.get(name) ?: defaultValue) as T
    }

    public override fun toString(): String {
        return "${this.javaClass}: " + data.toString()
    }
}