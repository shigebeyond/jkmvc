package net.jkcode.jkmvc.es


import java.util.ArrayList
import java.util.HashMap

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * 使用Gson把json字符串转成Map
 * @author lianqiang
 * @date 2014/06/12
 */
object JsonToMap {

    /**
     * 获取JsonObject
     * @param json
     * @return
     */
    fun parseJson(json: String): JsonObject {
        val parser = JsonParser()
        return parser.parse(json).asJsonObject
    }

    /**
     * 根据json字符串返回Map对象
     * @param json
     * @return
     */
    fun toMap(json: String): Map<String, Any> {
        return toMap(parseJson(json))
    }

    /**
     * 将JSONObjec对象转换成Map-List集合
     * @param json
     * @return
     */
    fun toMap(json: JsonObject): Map<String, Any> {
        val map = HashMap<String, Any>()
        val entrySet = json.entrySet()
        val iter = entrySet.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val key = entry.key
            val value = entry.value
            if (value is JsonArray)
                map[key as String] = toList(value)
            else if (value is JsonObject)
                map[key as String] = toMap(value)
            else
                map[key as String] = value
        }
        return map
    }

    /**
     * 将JSONArray对象转换成List集合
     * @param json
     * @return
     */
    fun toList(json: JsonArray): List<Any> {
        val list = ArrayList<Any>()
        for (i in 0 until json.size()) {
            val value = json.get(i)
            if (value is JsonArray) {
                list.add(toList(value))
            } else if (value is JsonObject) {
                list.add(toMap(value))
            } else {
                list.add(value)
            }
        }
        return list
    }
}