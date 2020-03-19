package net.jkcode.jkmvc.orm

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature

/**
 * 标准化orm数据
 *    对orm对象要转map
 * @param data
 * @param include 要输出的字段名的列表
 * @return
 */
public fun normalizeOrmData(data: Any?, include: List<String> = emptyList()): Any? {
    // 对orm对象要转map
    if (data is IOrm)
        return data.toMap(include)

    // 对orm列表要转map
    if(data is List<*> && data.isNotEmpty() && data.first() is IOrm)
        return (data as List<IOrm>).itemToMap(include)

    return data
}

/**
 * 对象转json
 * @param include 要输出的字段名的列表
 * @return
 */
public fun Any.toJson(include: List<String> = emptyList()): String {
    //data.toJSONString()
    val data = normalizeOrmData(this, include)
    return JSON.toJSONString(data, SerializerFeature.WriteDateUseDateFormat /* Date格式化 */, SerializerFeature.WriteMapNullValue /* 输出null值 */)
}