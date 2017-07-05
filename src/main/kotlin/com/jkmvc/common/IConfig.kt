package com.jkmvc.common


import java.io.InputStream
import java.util.*

/**
 * 配置数据，用于加载配置文件，并读取配置数据
 * Config data, can load properties file from CLASSPATH or File object.
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
abstract class IConfig {

    /**
     * 配置项
     */
    protected lateinit var props: Properties;

    /**
     * 加载配置文件
     */
    public abstract fun load(inputStream: InputStream?, encoding: String = "UTF-8");

    /**
     * 获得配置项的值，自动转换为指定类型
     */
    public operator inline fun <reified T:Any> get(key: String, defaultValue: T? = null): T?{
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得string类型的配置项
     */
    public abstract fun getString(key: String, defaultValue: String?): String?;
    
    /**
     * 获得int类型的配置项
     */
    public abstract fun getInt(key: String, defaultValue: Int? = null): Int?;

    /**
     * 获得long类型的配置项
     */
    public abstract fun getLong(key: String, defaultValue: Long? = null): Long?;


    /**
     * 获得float类型的配置项
     */
    public abstract fun getFloat(key: String, defaultValue: Float?): Float?;

    /**
     * 获得Double类型的配置项
     */
    public abstract fun getDouble(key: String, defaultValue: Double?): Double?;
    
    /**
     * 获得bool类型的配置项
     */
    public abstract fun getBoolean(key: String, defaultValue: Boolean? = null): Boolean?;

    /**
     * 获得short类型的配置项
     */
    public abstract fun getShort(key: String, defaultValue: Short? = null): Short?;

    /**
     * 获得Date类型的配置项
     */
    public abstract fun getDate(key: String, defaultValue: Date?): Date?;
    /**
     * 判断是否含有配置项
     */
    public abstract fun containsKey(key: String): Boolean;
}
