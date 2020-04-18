package net.jkcode.jkmvc.orm

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import net.jkcode.jkutil.common.getPathPropertyValue
import net.jkcode.jkutil.common.getPropertyValue

/**
 * 标准化数据
 *    对象要转map
 * @param data
 * @param include 要输出的字段名的列表
 * @return
 */
public fun normalizeData(data: Any?, include: List<String> = emptyList()): Any? {
    if(data == null)
        return null

    // 1 对象集合
    if(data is Collection<*>)
        return (data as Collection<Any>).toMaps(include)

    // 2 对象
    return data.toMap(include)
}

/**
 * 对象转map
 * @param include
 * @return
 */
public fun Any.toMap(include: List<String> = emptyList()): MutableMap<String, Any?> {
    // 1 orm对象
    // 问题: IOrmEntity.toMap() 只能转内部属性 _data, 不能转getter方法, 而且不能支持多级属性
    if (this is IOrm && include.isEmpty())
        return this.toMap()

    // 2 普通对象
    return include.associate { prop ->
        val value = this.getPathPropertyValue(prop) // 支持多级属性
        prop to value
    } as MutableMap
}

/**
 * 集合转map
 * * @param include
 * @return
 */
public fun Collection<*>.toMaps(include: List<String> = emptyList()): List<MutableMap<String, Any?>>{
    if(this.isEmpty())
        return emptyList()

    return this.map {
        (it as Any).toMap(include)
    }
}

/**
 * 对象转json
 * @param include 要输出的字段名的列表
 * @return
 */
public fun Any.toJson(include: List<String> = emptyList()): String {
    //data.toJSONString()
    val data = if(include.isEmpty())
                    this
                else
                    normalizeData(this, include)
    return JSON.toJSONString(data, SerializerFeature.WriteDateUseDateFormat /* Date格式化 */, SerializerFeature.WriteMapNullValue /* 输出null值 */)
}