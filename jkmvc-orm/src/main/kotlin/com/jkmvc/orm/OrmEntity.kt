package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.common.*
import net.jkcode.jkmvc.db.MutableRow
import net.jkcode.jkmvc.db.Row
import net.jkcode.jkmvc.serialize.ISerializer
import java.math.BigDecimal
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

/**
 * ORM之实体对象
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
abstract class OrmEntity : IOrm {

    companion object{

        /**
         * orm配置
         */
        public val config: Config = Config.instance("orm")

        /**
         * 序列化
         */
        public val serializer: ISerializer = ISerializer.instance(config["serializeType"]!!)

        /**
         * 缓存属性代理
         */
        public val prop = (object : ReadWriteProperty<IOrm, Any?> {
            // 获得属性
            public override operator fun getValue(thisRef: IOrm, property: KProperty<*>): Any? {
                return thisRef[property.name]
            }

            // 设置属性
            public override operator fun setValue(thisRef: IOrm, property: KProperty<*>, value: Any?) {
                thisRef[property.name] = value
            }
        })
    }

    /**
     * 最新的字段值：<字段名 to 最新字段值>
     */
    protected val data: MutableRow = HashMap<String, Any?>()

    /**
     * 变化的字段值：<字段名 to 原始字段值>
     *     一般只读，lazy创建，节省内存
     */
    protected val dirty: MutableRow by lazy {
        HashMap<String, Any?>()
    };

    /**
     * 获得属性代理
     */
    public override fun <T> property(): ReadWriteProperty<IOrm, T> {
        return prop as ReadWriteProperty<IOrm, T>;
    }

    /**
     * 判断是否有某字段
     *
     * @param column
     * @return
     */
    public override fun hasColumn(column: String): Boolean {
        return true;
    }

    /**
     * 设置对象字段值
     *
     * @param column 字段名
     * @param  value  字段值
     */
    public override operator fun set(column: String, value: Any?) {
        if (!hasColumn(column))
            throw OrmException("类 ${this.javaClass} 没有字段 $column");

        // 记录变化的字段名 + 原始值
        if(!dirty.containsKey(column)
                //&& value != data[column])
                && !equalsValue(data[column], value))
            dirty[column] = data[column];

        data[column] = value;
    }

    /**
     * 显式标记字段有变化
     * @param column 字段名
     */
    public override fun setDirty(column: String){
        dirty[column] = data[column];
    }

    /**
     * 判断属性值是否相等
     *    只在 set() 中调用，用于检查属性值是否修改
     *
     * @param oldValue
     * @param newValue
     * @return
     */
    protected fun equalsValue(oldValue: Any?, newValue: Any?): Boolean{
        if(oldValue == newValue) // 相等
            return true

        if(oldValue == null || newValue == null) // 不等，却有一个为空
            return false

        if(oldValue is BigDecimal && newValue !is BigDecimal) // 不等，却是 BigDecimal 与 其他数值类型
            return oldValue.toNumber(newValue.javaClass) == newValue // 由于只在 set() 调用，所以假定oldValue转为newValue的类型时，不丢失精度

        return false
    }

    /**
     * 获得对象字段
     *
     * @param column 字段名
     * @param defaultValue 默认值
     * @return
     */
    public override operator fun <T> get(column: String, defaultValue: T?): T {
        if (!hasColumn(column))
            throw OrmException("类 ${this.javaClass} 没有字段 $column");

        return (data[column] ?: defaultValue) as T
    }

    /**
     * 获得或设置字段值
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public inline fun getOrPut(key: String, defaultValue: () -> Any?): Any? {
        // 获得字段值
        val value = data[key]
        if (value != null)
            return value

        // 设置字段值
        val answer = defaultValue()
        set(key, answer)
        return answer
    }

    /**
     * 设置多个字段值
     *
     * @param values   字段值的数组：<字段名 to 字段值>
     * @param expected 要设置的字段名的数组
     * @return
     */
    public override fun values(values: Row, expected: List<String>?): IOrm {
        if(values.isEmpty())
            return this

        val columns = if (expected.isNullOrEmpty())
            values.keys
        else
            expected!!;

        for (column in columns)
            this[column] = values[column];

        return this;
    }

    /**
     * 智能设置属性
     *    1 智能化
     *    在不知属性类型的情况下，将string赋值给属性
     *    => 需要将string转换为属性类型
     *    => 需要显式声明属性
     *
     *    2 不确定性
     *    一般用于从Request对象中批量获得属性，即获得与请求参数同名的属性值
     *    但是请求中可能带其他参数，不一定能对应到该对象的属性名，因此不抛出异常，只返回bool
     *
     * <code>
     *     class UserModel(id:Int? = null): Orm(id) {
     *          ...
     *          public var id:Int by property<Int>(); //需要显式声明属性
     *     }
     *
     *     val user = UserModel()
     *     user.id = String.parseInt("123")
     *     // 相当于
     *     user.setIntelligent("id", "123")
     * </code>
     *
     * @param column
     * @param value 字符串
     * @return
     */
    public override fun setIntelligent(column:String, value:String): Boolean {
        if (!hasColumn(column))
            return false;

        // 1 获得属性
        val prop = this::class.getProperty(column) as KMutableProperty1?
        if(prop == null)
            return false

        try {
            // 2 准备参数: 转换类型
            val param = value.to(prop.setter.parameters[1].type)
            // 3 调用setter方法
            prop.setter.call(this, param);
            return true
        }catch (e: Exception){
            throw OrmException("智能设置属性[$column=$value]错误: ${e.message}", e)
        }
    }

    /**
     * 序列化
     * @return
     */
    public override fun serialize(): ByteArray? {
        return serializer.serialize(this.toMap())
    }

    /**
     * 序列化
     *
     * @param bytes
     */
    public override fun unserialize(bytes: ByteArray): Unit {
        data.putAll(serializer.unserizlize(bytes) as Map<String, Any?>)
    }

    /**
     * 编译字符串模板
     *
     * @param template 字符串模板，格式 "name=:name, age=:age"，用:来修饰字段名
     * @return 将模板中的字段名替换为字段值
     */
    public override fun compileTemplate(template:String):String{
        // 1 编译模板
        if(template.contains(':'))
            return template.replaces(data);

        // 2 输出单个字段
        return data[template].toString()
    }

}
