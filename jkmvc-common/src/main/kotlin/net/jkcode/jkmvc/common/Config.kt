package net.jkcode.jkmvc.common


import com.alibaba.fastjson.JSONObject
import net.jkcode.jkmvc.singleton.BeanSingletons
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

/**
 * 配置数据，用于加载配置文件，并读取配置数据
 * Config data, can load properties file from CLASSPATH or File object.
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
class Config(public override val props: Map<String, *>, // 配置项
             public override val file: String = "", // 配置文件
             public override val merging: Boolean = false // 是否合并
): IConfig(){

    companion object{
        /**
         * 缓存配置数据
         *   key 文件名
         *   value 配置数据
         */
        val configs:ConcurrentHashMap<String, Config?> = ConcurrentHashMap<String, Config?>()

        /**
         * 获得配置数据，如果没有数据，则加载配置文件来读取数据
         * Get the config data. It will loading the properties file if not loading.
         *
         * 例子：
         * <code>
         *      val config = Config.instance("config.txt", "UTF-8");
         *      String username = config.get("username");
         *      String password = config.get("password");
         *
         *      username = Config.instance("other_config.txt").get("username");
         *      password = Config.instance("other_config.txt").get("password");
         *
         *      Config.instance("com/jfinal/config_in_sub_directory_of_classpath.txt");
         * <code>
         *
         * @param file the properties file's name in classpath or the sub directory of classpath
         * @param type properties | yaml
         * @param merging
         */
        @JvmStatic
        @JvmOverloads
        public fun instance(file: String, type: String = "properties", merging: Boolean = false): Config {
            // 解析出文件名 + 子项路径
            var filename:String = file
            var path:String? = null
            val i = file.indexOf('.')
            if(i > -1){
                filename = file.substring(0, i)
                path = file.substring(i + 1)
            }
            // 获得文件的配置项
            val config = configs.getOrPutOnce(filename){
                Config("$filename.$type", type, merging)
            }!!
            if(config.merging != merging)
                throw IllegalArgumentException("配置文件[$file]已加载过, 但本地加载与之前加载的 merging 参数不一样")

            // 无子项
            if(path == null)
                return config
            // 有子项
            return config.pathConfig(path)
        }

        /**
         * 构建配置项
         *
         * @param file the properties file's name in classpath or the sub directory of classpath
         * @param type properties | yaml | json
         * @param merging
         * @return
         */
        public fun buildProperties(file:String, type: String = "properties", merging: Boolean = false): Map<String, *> {
            val urls = Thread.currentThread().contextClassLoader.getResources(file)
            if(!urls.hasMoreElements())
                throw IllegalArgumentException("配置文件[$file]不存在")
            // 1 不合并: 取第一个
            if (!merging)
                return buildProperties(urls.nextElement(), type)

            // 2 合并: 先来先写, 后来不覆盖
            val result = HashMap<String, Any?>()
            for (url in urls){
                val props = buildProperties(url, type)
                // 合并, 但不覆盖先前的
                result.putAllIfAbsent(props)
            }
            return result
        }

        /**
         * 构建配置项
         *
         * @param url　配置文件
         * @param type properties | yaml | json
         * @return
         */
        public fun buildProperties(url: URL, type: String = "properties"): Map<String, *> {
            val inputStream = url.openStream()
            // 无内容则返回空map
            if(inputStream.available() == 0)
                return emptyMap<String, Any?>()

            // 解析内容
            val result = inputStream.use {
                when(type){
                    "properties" -> Properties().apply { load(InputStreamReader(inputStream, "UTF-8")) } // 加载 properties 文件
                    "yaml" -> Yaml().loadAs(inputStream, HashMap::class.java) // 加载 yaml 文件
                    "json" -> JSONObject.parseObject(inputStream.reader().readText()) // 加载 json 文件
                    else -> throw IllegalArgumentException("未知配置文件类型")
                }
            }
            if(result == null)
                return emptyMap<String, Any?>()

            return result as Map<String, *>
        }
    }

    /**
     * 例子：
     * <code>
     *      val config = Config("my_config.properties");
     *      val config = Config("my_config.properties", "properties");
     *      val config = Config("my_config.properties", "yaml");
     *      val username = config.get("username");
     * <code>
     *
     * @param file the properties file's name in classpath or the sub directory of classpath
     * @param type properties | yaml
     * @param　merging
     */
    public constructor(file: String, type: String = "properties", merging: Boolean = false):this(buildProperties(file, type, merging), file, merging){
    }

    /**
     * 判断是否含有配置项
     * @param key
     * @return
     */
    public override fun containsKey(key: String): Boolean {
        return props.containsKey(key)
    }

    /**
     * 获得string类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun getString(key: String, defaultValue: String?): String? {
        val value = props.get(key)
        return if(value == null)
            defaultValue
        else
            value.toString()
    }

    /**
     * 获得int类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun getInt(key: String, defaultValue: Int?): Int? {
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得long类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun getLong(key: String, defaultValue: Long?): Long? {
        return props.getAndConvert(key, defaultValue)
    }


    /**
     * 获得float类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun getFloat(key: String, defaultValue: Float?): Float? {
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得double类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun getDouble(key: String, defaultValue: Double?): Double? {
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得bool类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun getBoolean(key: String, defaultValue: Boolean?): Boolean? {
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得short类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun getShort(key: String, defaultValue: Short?): Short?{
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得Date类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun getDate(key: String, defaultValue: Date?): Date?{
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得Map类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun getMap(key: String, defaultValue: Map<String, *>?): Map<String, *>?{
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得List类型的配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun getList(key: String, defaultValue: List<*>?): List<*>?{
        return props.getAndConvert(key, defaultValue)
    }

    /**
     * 获得Config类型的子配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun pathConfig(path: String): Config{
        try{
            val subprops = props.path(path) as Map<String, *>
            return Config(subprops)
        }catch (e:ClassCastException){
            throw NoSuchElementException("构建配置子项失败：配置数据为$props, 但路径[$path]的子项不是Map")
        }
    }

    /**
     * 获得Properties类型的子配置项
     * @param key
     * @param defaultValue
     * @return
     */
    public override fun pathProperties(path: String): Properties{
        try{
            val subprops = props.path(path) as Map<String, *>
            val result = Properties()
            result.putAll(subprops)
            return result
        }catch (e:ClassCastException){
            throw NoSuchElementException("构建配置子项失败：配置数据为$props, 但路径[$path]的子项不是Map")
        }
    }

    /**
     * 配置项是类的列表, 对应返回实例列表
     * @param prop
     * @return
     */
    public override fun <T> classes2Instances(prop: String): List<T>{
        val classes: List<String>? = this[prop]
        if(classes.isNullOrEmpty())
            return LinkedList() // 空也返回可写的list, 外面可能要用到, 特别是对配置的插件/拦截器列表而言

        return classes!!.map { clazz ->
            BeanSingletons.instance(clazz) as T
        }
    }

    public override fun toString(): String {
        return "Config[$props]"
    }
}
