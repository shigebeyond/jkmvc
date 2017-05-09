package com.jkmvc.common


import java.io.InputStream

/**
 * Config data, can load properties file from CLASSPATH or File object.
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
interface IConfig {

    /**
     * 加载配置文件
     */
    fun load(inputStream: InputStream?, encoding: String = "UTF-8");

    /**
     * 获得配置项
     */
    operator fun get(key: String, defaultValue: String? = null): String?;

    /**
     * 获得int类型的配置项
     */
    fun getInt(key: String, defaultValue: Int? = null): Int?;

    /**
     * 获得long类型的配置项
     */
    fun getLong(key: String, defaultValue: Long? = null): Long?;

    /**
     * 获得bool类型的配置项
     */
    fun getBoolean(key: String, defaultValue: Boolean? = null): Boolean?;

    /**
     * 判断是否含有配置项
     */
    fun containsKey(key: String): Boolean;
}
