package com.jkmvc.common


import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Config data, can load properties file from CLASSPATH or File object.
 */
class Config {

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
            return configs.getOrPut(fileName){
                Config(fileName, encoding)
            }
        }
    }

    protected lateinit var props: Properties;

    /**
     * Prop constructor
     *
     *
     * Example:
     * Prop prop = new Prop("my_config.txt", "UTF-8");
     * String userName = prop.get("userName");

     * prop = new Prop("com/jfinal/file_in_sub_path_of_classpath.txt", "UTF-8");
     * String value = prop.get("key");

     * @param fileName the properties file's name in classpath or the sub directory of classpath
     * *
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
     * Prop prop = new Prop(new File("/var/config/my_config.txt"), "UTF-8");
     * String userName = prop.get("userName");

     * @param file the properties File object
     * *
     * @param encoding the encoding
     */
    constructor(file: File?, encoding: String = "UTF-8") {
        if (file == null)
            throw IllegalArgumentException("File can not be null.")

        if (file.isFile == false)
            throw IllegalArgumentException("File not found : " + file.name)

        load(FileInputStream(file), encoding);
    }

    protected fun load(inputStream: InputStream?, encoding: String = "UTF-8") {
        if(inputStream == null)
            throw IllegalArgumentException("InputStream is null")

        try {
            props = Properties()
            props.load(InputStreamReader(inputStream, encoding))
        } finally {
            inputStream.close()
        }
    }

    public operator fun get(key: String, defaultValue: String? = null): String? {
        return props.getProperty(key, defaultValue)
    }

    public fun getInt(key: String, defaultValue: Int? = null): Int? {
        val value = props.getProperty(key)
        return if(value == null)
                    defaultValue
                else
                    value.toInt();
    }

    public fun getLong(key: String, defaultValue: Long? = null): Long? {
        val value = props.getProperty(key)
        return if(value == null)
                    defaultValue
                else
                    value.toLong();
    }

    public fun getBoolean(key: String, defaultValue: Boolean? = null): Boolean? {
        var value: String? = props.getProperty(key)
        return if(value == null)
                    defaultValue
                else
                    value.toBoolean();
    }

    public fun containsKey(key: String): Boolean {
        return props.containsKey(key)
    }
}
