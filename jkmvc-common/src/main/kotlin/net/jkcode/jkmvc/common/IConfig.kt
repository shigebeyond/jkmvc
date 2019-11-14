package net.jkcode.jkmvc.common


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
     * 配置文件
     */
    public abstract val file: String

    /**
     * 配置项
     */
    public abstract val props: Map<String, *>;

    /**
     * 是否合并
     */
    public abstract val merging: Boolean

    /**
     * 判断是否含有配置项
     * @param key
     * @return
     */
    public abstract fun containsKey(key: String): Boolean;

    /**
     * 获得配置项的值
     *    注：调用时需明确指定返回类型，来自动转换参数值为指定类型
     * @param key
     * @param defaultValue
     * @return
     */
    public operator inline fun <reified T:Any> get(key: String, defaultValue: T? = null): T?{
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得string类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun getString(key: String, defaultValue: String? = null): String?;
    
    /**
     * 获得int类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun getInt(key: String, defaultValue: Int? = null): Int?;

    /**
     * 获得long类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun getLong(key: String, defaultValue: Long? = null): Long?;


    /**
     * 获得float类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun getFloat(key: String, defaultValue: Float?): Float?;

    /**
     * 获得Double类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun getDouble(key: String, defaultValue: Double?): Double?;
    
    /**
     * 获得bool类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun getBoolean(key: String, defaultValue: Boolean? = null): Boolean?;

    /**
     * 获得short类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun getShort(key: String, defaultValue: Short? = null): Short?;

    /**
     * 获得Date类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun getDate(key: String, defaultValue: Date? = null): Date?;

    /**
     * 获得Map类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun getMap(key: String, defaultValue: Map<String, *>? = null): Map<String, *>?

    /**
     * 获得List类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun getList(key: String, defaultValue: List<*>? = null): List<*>?

    /**
     * 获得Config类型的子配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun pathConfig(path: String): Config;

    /**
     * 获得Properties类型的子配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public abstract fun pathProperties(path: String): Properties

    /**
     * 配置项是类的列表, 对应返回实例列表
     * @param prop
     * @return
     */
    public abstract fun <T> classes2Instances(prop: String): List<T>
}
