package com.jkmvc.orm

import com.jkmvc.common.findProperty
import com.jkmvc.common.isNullOrEmpty
import com.jkmvc.common.to
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
         * 缓存属性代理
         */
        protected val prop = (object : ReadWriteProperty<IOrm, Any?> {
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

    protected val data: MutableMap<String, Any?> = HashMap<String, Any?>()

    /**
     * 变化的字段值：<字段名 to 字段值>
     */
    protected val dirty: MutableSet<String> by lazy {
        HashSet<String>()
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

        dirty.add(column);
        data[column] = value;
    }

    /**
     * 获得对象字段
     *
     * @param column 字段名
     * @param defaultValue 默认值
     * @return
     */
    public override operator fun <T> get(column: String, defaultValue: Any?): T {
        if (!hasColumn(column))
            throw OrmException("类 ${this.javaClass} 没有字段 $column");

        return (data[column] ?: defaultValue) as T
    }

    /**
     * 设置多个字段值
     *
     * @param values   字段值的数组：<字段名 to 字段值>
     * @param expected 要设置的字段名的数组
     * @return
     */
    public override fun values(values: Map<String, Any?>, expected: List<String>?): IOrm {
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
     */
    public override fun setIntelligent(column:String, value:String)
    {
        // 1 获得属性
        val prop = this::class.findProperty(column) as KMutableProperty1
        if(prop == null)
            throw OrmException("类 ${this.javaClass} 没有属性: $column");

        // 2 准备参数: 转换类型
        val param = value.to(prop.setter.parameters[1].type)
        // 3 调用setter方法
        prop.setter.call(this, param);
    }

    /**
     * 获得字段值
     * @return
     */
    public override fun asMap(): Map<String, Any?> {
        return data;
    }

}
