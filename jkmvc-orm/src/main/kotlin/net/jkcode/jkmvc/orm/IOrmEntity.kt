package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.db.Row
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
     * @param defaultValue 默认值
     * @return
     */
    operator fun <T> get(column: String, defaultValue: T? = null): T;

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
     * 设置多个字段值
     *
     * @param values   字段值的数组：<字段名 to 字段值>
     * @param expected 要设置的字段名的数组
     * @return
     */
    fun values(values: Row, expected: List<String>? = null): IOrmEntity;

    /**
     * 获得字段值
     * @return
     */
    fun toMap(): Map<String, Any?>;

    /**
     * 从map中设置字段值
     *
     * @param data
     */
    fun fromMap(data: Map<String, Any?>): Unit;

    /**
     * 从其他实体对象中设置字段值
     *
     * @param data
     */
    fun from(other: IOrmEntity): Unit{
        fromMap(other.toMap())
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
    fun unserialize(bytes: ByteArray): Unit

    /**
     * 编译字符串模板
     *
     * @param template 字符串模板，格式 "name=:name, age=:age"，用:来修饰字段名
     * @return 将模板中的字段名替换为字段值
     */
    fun compileTemplate(template:String):String

}