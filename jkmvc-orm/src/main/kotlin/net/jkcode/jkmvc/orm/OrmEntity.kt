package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.common.*
import net.jkcode.jkmvc.db.MutableRow
import net.jkcode.jkmvc.serialize.ISerializer
import java.math.BigDecimal
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

/**
 * ORM之实体对象
 *  1. 本来想继承 MutableMap<String, Any?>, 但是得不偿失, 不值得做
 *    仅仅需要的是get()/put()
 *    可有可无的是size()/isEmpty()/containsKey()/containsValue()
 *    完全不需要的是remove()/clear()/keys/values/entries/MutableEntry
 *
 *  2. data 属性的改写
 *  2.1 子类 OrmValid 中改写
 *      改写为 net.jkcode.jkmvc.common.FixedKeyMapFactory.FixedKeyMap
 *      由于是直接继承 OrmEntity 来改写的, 因此直接覆写 data 属性, 因此能够应用到依赖 data 属性的方法
 *
 *  2.2 在实体类 XXXEntity 与模型类 XXXModel 分离的场景下改写, 如:
 *      XXXEntity: open class MessageEntity: OrmEntity()
 *      XXXModel: class MessageModel: MessageEntity(), IOrm by GeneralModel(m)
 *      而 XXXModel 继承于 XXXEntity 是为了继承与复用其声明的属性, 但是 IOrm 的方法全部交由 GeneralModel 代理来改写, 也就对应改写掉 XXXEntity/OrmEntity 中与 IOrm 重合的方法(即 IOrmEntity 的方法)
 *      但是注意某些方法与属性是 XXXEntity/OrmEntity 特有的, 没有归入 IOrm 接口, 也就是说 GeneralModel 不能改写这些方法与属性
 *      如 data 是内部属性无法被 IOrm 接口暴露
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
open class OrmEntity : IOrmEntity {

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
        public val prop = (object : ReadWriteProperty<IOrmEntity, Any?> {
            // 获得属性
            public override operator fun getValue(thisRef: IOrmEntity, property: KProperty<*>): Any? {
                return thisRef[property.name]
            }

            // 设置属性
            public override operator fun setValue(thisRef: IOrmEntity, property: KProperty<*>, value: Any?) {
                thisRef[property.name] = value
            }
        })
    }

    /**
     * 最新的字段值：<字段名 to 最新字段值>
     * 1 子类会改写
     * 2 延迟加载, 对于子类改写是没有意义的, 但针对实体类 XXXEntity 与模型类 XXXModel 分离的场景下是有意义的, 也就是IOrm 的方法全部交由 GeneralModel 代理来改写, 也就用不到该类的 data 属性
     */
    protected open val data: MutableRow by lazy{
        HashMap<String, Any?>()
    }

    /**
     * 获得属性代理
     */
    public fun <T> property(): ReadWriteProperty<IOrmEntity, T> {
        return prop as ReadWriteProperty<IOrmEntity, T>;
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
     *    子类会改写
     *
     * @param column 字段名
     * @param  value  字段值
     */
    public override operator fun set(column: String, value: Any?) {
        data[column] = value;
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
     *    子类会改写
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
     * 暴露data属性, 仅限orm模块使用
     * @return
     */
    internal fun getData(): MutableRow {
        return data
    }

    /**
     * 从其他实体对象中设置字段值
     *   子类会改写
     * @param from
     */
    public override fun from(from: IOrmEntity): Unit{
        from.toMap(data)
    }

    /**
     * 从map中设置字段值
     *   子类会改写
     * @param from   字段值的哈希：<字段名 to 字段值>
     * @param expected 要设置的字段名的列表
     */
    public override fun fromMap(from: Map<String, Any?>, expected: List<String>): Unit {
        copyMap(from, data, expected)
    }

    /**
     * 获得字段值 -- 转为Map
     *     子类会改写
     * @param to
     * @param expected 要设置的字段名的列表
     * @return
     */
    public override fun toMap(to: MutableMap<String, Any?>, expected: List<String>): MutableMap<String, Any?> {
        return copyMap(data, to, expected)
    }

    /**
     * 从from中复制字段值到to
     *
     * @param from 源map
     * @param to 目标map
     * @param expected 要设置的字段名的列表
     * @return
     */
    protected fun copyMap(from: Map<String, Any?>, to: MutableMap<String, Any?>, expected: List<String>): MutableMap<String, Any?> {
        val columns = if (expected.isEmpty())
                        from.keys
                    else
                        expected

        for (column in columns)
            to[column] = from[column]

        return to
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

    /**
     * 转字符串
     * @return
     */
    public override fun toString(): String {
        return "${this.javaClass}: $data"
    }

}
