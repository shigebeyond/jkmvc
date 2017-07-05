package com.jkmvc.common


import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import com.jkmvc.common.getOrDefault

/**
 * 配置数据，用于加载配置文件，并读取配置数据
 * Config data, can load properties file from CLASSPATH or File object.
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
class Config:IConfig {

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
         * @param encoding the encoding
         */
        public fun instance(fileName: String, encoding: String = "UTF-8"): Config? {
            val fullName = "$fileName.properties";
            return configs.getOrPut(fullName){
                Config(fullName, encoding)
            }
        }
    }

    /**
     * Config constructor
     *
     *
     * Example:
     * val config = Config("my_config.txt", "UTF-8");
     * val userName = config.get("userName");

     * @param fileName the properties file's name in classpath or the sub directory of classpath
     * @param encoding the encoding
     */
    constructor(fileName: String, encoding: String = "UTF-8") {
        var inputStream: InputStream? = Thread.currentThread().contextClassLoader.getResourceAsStream(fileName)        // properties.load(Prop.class.getResourceAsStream(fileName));
        if (inputStream == null)
            throw IllegalArgumentException("Properties file not found in classpath: " + fileName)

        load(inputStream, encoding);
    }

    /**
     * Prop constructor
     *
     *
     * Example:
     * val prop = Prop(new File("/var/config/my_config.txt"), "UTF-8");
     * val userName = prop.get("userName");

     * @param file the properties File object
     * @param encoding the encoding
     */
    constructor(file: File?, encoding: String = "UTF-8") {
        if (file == null)
            throw IllegalArgumentException("File can not be null.")

        if (file.isFile == false)
            throw IllegalArgumentException("File not found : " + file.name)

        load(FileInputStream(file), encoding);
    }

    /**
     * 加载配置文件
     */
    public override fun load(inputStream: InputStream?, encoding: String) {
        if(inputStream == null)
            throw IllegalArgumentException("InputStream is null")

        try {
            props = Properties()
            props.load(InputStreamReader(inputStream, encoding))
        } finally {
            inputStream.close()
        }
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
     * 判断是否含有配置项
     */
    public override fun containsKey(key: String): Boolean {
        return props.containsKey(key)
    }
}
