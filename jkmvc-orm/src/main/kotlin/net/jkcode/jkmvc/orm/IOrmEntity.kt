package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.db.DbResultRow
import net.jkcode.jkmvc.orm.prop.OrmListPropDelegater
import net.jkcode.jkmvc.orm.prop.OrmMapPropDelegater
import net.jkcode.jkmvc.orm.prop.OrmPropDelegater
import net.jkcode.jkmvc.orm.prop.OrmSetPropDelegater
import java.util.*
import kotlin.collections.HashMap
import kotlin.properties.ReadWriteProperty

/**
 * ORM之实体对象
 *  1. 本来想继承 MutableMap<String, Any?>, 但是得不偿失, 不值得做
 *    仅仅需要的是get()/put()
 *    可有可无的是size()/isEmpty()/containsKey()/containsValue()
 *    完全不需要的是remove()/clear()/keys/values/entries/MutableEntry
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
interface IOrmEntity {

    /**
     * 获得属性代理
     * @return
     */
    public fun <T> property(): ReadWriteProperty<IOrmEntity, T> {
        return OrmPropDelegater as ReadWriteProperty<IOrmEntity, T>;
    }

    /**
     * 获得列表属性代理
     * @return
     */
    public fun <T: List<*>> listProperty(): ReadWriteProperty<IOrmEntity, T> {
        return OrmListPropDelegater as ReadWriteProperty<IOrmEntity, T>;
    }

    /**
     * 获得集合属性代理
     * @return
     */
    public fun <T: Set<*>> setProperty(): ReadWriteProperty<IOrmEntity, T> {
        return OrmSetPropDelegater as ReadWriteProperty<IOrmEntity, T>;
    }

    /**
     * 获得哈希属性代理
     * @param keys 作为键的字段名
     * @return
     */
    public fun <T: Map<*, *>> mapProperty(vararg keys: String): ReadWriteProperty<IOrmEntity, T> {
        return OrmMapPropDelegater.instance(DbKeyNames(*keys)) as ReadWriteProperty<IOrmEntity, T>;
    }

    /**
     * 判断是否有某字段
     *
     * @param column
     * @return
     */
    fun hasColumn(column: String): Boolean;

    /**
     * 获得对象字段
     *
     * @param column 字段名
     * @return
     */
    operator fun <T> get(column: String): T;

    /**
     * 获得或设置字段值
     *
     * @param key
     * @param defaultValue
     * @return
     */
    fun getOrPut(key: String, defaultValue: () -> Any?): Any? {
        // 获得字段值
        var value: Any? = get(key)
        if (value == null) {
            // 设置字段值
            value = defaultValue()
            set(key, value)
        }

        return value
    }

    /**
     * 设置对象字段值
     *
     * @param column 字段名
     * @param  value  字段值
     */
    operator fun set(column: String, value: Any?);

    /**
     * 智能设置属性
     *    在不知属性类型的情况下，将string赋值给属性
     *    => 需要将string转换为属性类型
     *    => 需要显式声明属性
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
    fun setIntelligent(column:String, value:String):Boolean

    /**
     * 清空字段值
     * @return
     */
    fun clear()

    /**
     * 从结果行中设置字段值
     * @param row 结果行
     * @param convertingColumn 是否转换字段名
     */
    fun fromRow(row: DbResultRow, convertingColumn: Boolean = false)

    /**
     * 从map中设置字段值
     *
     * @param from   字段值的哈希：<字段名 to 字段值>
     * @param include 要设置的字段名的列表
     * @param exclude 要排除的字段名的列表
     * @param includeRelated 是否包含关联属性, 仅当 include 为空时有效
     */
    fun fromMap(from: Map<String, Any?>, include: List<String> = emptyList(), exclude: List<String> = emptyList(), includeRelated: Boolean = true)

    /**
     * 获得字段值 -- 转为Map
     * @param to
     * @param include 要设置的字段名的列表
     * @param exclude 要排除的字段名的列表
     * @return
     */
    fun toMap(to: MutableMap<String, Any?> = HashMap(), include: List<String> = emptyList(), exclude: List<String> = emptyList()): MutableMap<String, Any?>;

    /**
     * 获得字段值 -- 转为Map
     * @param include 要设置的字段名的列表
     * @param exclude 要排除的字段名的列表
     * @return
     */
    fun toMap(include: List<String>, exclude: List<String> = emptyList()): MutableMap<String, Any?>{
        return toMap(HashMap(), include, exclude)
    }

    /**
     * 序列化
     * @return
     */
    fun serialize(): ByteArray?

    /**
     * 序列化
     *
     * @param bytes
     */
    fun unserialize(bytes: ByteArray)

    /**
     * 编译字符串模板
     *
     * @param template 字符串模板，格式 "name=:name, age=:age"，用:来修饰字段名
     * @return 将模板中的字段名替换为字段值
     */
    fun compileTemplate(template:String):String

    /**
     * 改写 toString()
     *   在实体类 XXXEntity 与模型类 XXXModel 分离的场景下改写 OrmEntity.toString(), 如:
     *   XXXEntity: open class MessageEntity: OrmEntity()
     *   XXXModel: class MessageModel: MessageEntity(), IOrm by GeneralModel(m)
     *   而 XXXModel 继承于 XXXEntity 是为了继承与复用其声明的属性, 但是 IOrm 的方法全部交由 GeneralModel 代理来改写, 也就对应改写掉 XXXEntity/OrmEntity 中与 IOrm 重合的方法(即 IOrmEntity 的方法)
     *   但是某些方法与属性是 XXXEntity/OrmEntity 特有的, 没有归入 IOrm 接口, 也就是说 GeneralModel 不能改写这些方法与属性
     *   => 将 toString() 归入 IOrm 接口
     */
    override fun toString(): String
}