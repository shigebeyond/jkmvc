package com.jkmvc.common


import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import com.jkmvc.common.getOrDefault
import org.yaml.snakeyaml.Yaml

/**
 * 配置数据，用于加载配置文件，并读取配置数据
 * Config data, can load properties file from CLASSPATH or File object.
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
class Config: IConfig{

    companion object{
        /**
         * 缓存配置数据
         *   key 文件名
         *   value 配置数据
         */
        val configs:ConcurrentHashMap<String, Config?> = ConcurrentHashMap<String, Config?>()

        /**
         * Using the properties file. It will loading the properties file if not loading.
         * <p>
         * Example:<br>
         * val config = Config.instance("config.txt", "UTF-8");<br>
         * String userName = config.get("userName");<br>
         * String password = config.get("password");<br><br>
         *
         * userName = Config.instance("other_config.txt").get("userName");<br>
         * password = Config.instance("other_config.txt").get("password");<br><br>
         *
         * Config.instance("com/jfinal/config_in_sub_directory_of_classpath.txt");
         *
         * @param fileName the properties file's name in classpath or the sub directory of classpath
         * @param type properties | yaml
         */
        public fun instance(fileName: String, type: String = "properties"): Config? {
            return configs.getOrPut(fileName){
                Config("$fileName.$type", type)
            }
        }
    }

    /**
     * Example:
     * val connfig = Config(new FileInputStream("/var/config/my_config.properties"), "properties");
     * val connfig = Config(new FileInputStream("/var/config/my_config.yaml"), "yaml");
     * val userName = config.get("userName");
     *
     * @param fileName the properties file's name in classpath or the sub directory of classpath
     * @param type properties | yaml
     */
    constructor(inputStream: InputStream, type: String = "properties"){
        when(type){
            "properties" -> loadProperties(inputStream)
            "yaml" -> loadYaml(inputStream)
            else -> throw IllegalArgumentException("Unknow porperty file type")
        }
    }

    /**
     * Example:
     * val config = Config("my_config.properties");
     * val config = Config("my_config.properties", "properties");
     * val config = Config("my_config.properties", "yaml");
     * val userName = config.get("userName");
     *
     * @param fileName the properties file's name in classpath or the sub directory of classpath
     * @param type properties | yaml
     */
    public constructor(fileName: String, type: String = "properties"):this(Thread.currentThread().contextClassLoader.getResourceAsStream(fileName), type){
    }

    /**
     * 加载 properties 文件
     */
    protected fun loadProperties(inputStream: InputStream){
        if(inputStream == null)
            throw IllegalArgumentException("InputStream is null")
        try {
            props = Properties()
            props.load(InputStreamReader(inputStream, "UTF-8"))
        } finally {
            inputStream.close()
        }
    }

    /**
     * 加载 yaml 文件
     */
    protected fun loadYaml(inputStream: InputStream){
        if(inputStream == null)
            throw IllegalArgumentException("InputStream is null")
        try {
            props = Yaml().loadAs(inputStream, Properties::class.java);
        } finally {
            inputStream.close()
        }
    }

    /**
     * 判断是否含有配置项
     */
    public override fun containsKey(key: String): Boolean {
        return props.containsKey(key)
    }

    /**
     * 获得string类型的配置项
     */
    public override fun getString(key: String, defaultValue: String?): String? {
        val value = props.getProperty(key)
        return if(value == null)
            defaultValue
        else
            value
    }

    /**
     * 获得int类型的配置项
     */
    public override fun getInt(key: String, defaultValue: Int?): Int? {
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得long类型的配置项
     */
    public override fun getLong(key: String, defaultValue: Long?): Long? {
        return props.getAndConvert(key, defaultValue)
    }


    /**
     * 获得float类型的配置项
     */
    public override fun getFloat(key: String, defaultValue: Float?): Float? {
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得double类型的配置项
     */
    public override fun getDouble(key: String, defaultValue: Double?): Double? {
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得bool类型的配置项
     */
    public override fun getBoolean(key: String, defaultValue: Boolean?): Boolean? {
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得short类型的配置项
     */
    public override fun getShort(key: String, defaultValue: Short?): Short?{
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得Date类型的配置项
     */
    public override fun getDate(key: String, defaultValue: Date?): Date?{
        return props.getAndConvert(key, defaultValue)
    }

}
