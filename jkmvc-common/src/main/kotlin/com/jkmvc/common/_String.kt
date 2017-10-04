package com.jkmvc.common

import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

/****************************** 字符串扩展 *******************************/
/**
 * 根据Unicode编码完美的判断中文汉字和符号
 */
public fun Char.isChinese(): Boolean {
    // 根据字节码判断
    // return this >= 0x4E00 &&  this <= 0x9FA5;
    // 根据UnicodeBlock来判断
    val ub = Character.UnicodeBlock.of(this)
    return ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || ub === Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || ub === Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
            || ub === Character.UnicodeBlock.GENERAL_PUNCTUATION
}

/**
 * StringBuilder扩展
 * 清空
 *
 * @return
 */
public inline fun StringBuilder.clear(): StringBuilder {
    return this.delete(0, this.length - 1)
}

/**
 * StringBuilder扩展
 *  删除最后的一段子字符串
 * @param str 要删除的子字符串
 * @return
 */
public inline fun StringBuilder.deleteSuffix(str:String):StringBuilder {
    if(this.endsWith(str)) {
        val start = length - str.length;
        delete(start, length);
    }
    return this;
}

/**
 * 首字母大写
 * @return
 */
public inline fun String.ucFirst(): String {
    val cs = this.toCharArray()
    if(cs[0] in 'a'..'z')
        cs[0] = cs[0] - 32
    return String(cs)
}

/**
 * 首字母小写
 * @return
 */
public inline fun String.lcFirst(): String {
    val cs = this.toCharArray()
    if(cs[0] in 'A'..'Z')
        cs[0] = cs[0] + 32
    return String(cs)
}

/**
 * 去掉两头的字符
 *
 * @param str 要去掉的字符串
 * @return
 */
public inline fun String.trim(str:String): String {
    var start = 0;
    var end = length
    if(this.startsWith(str))
        start = str.length;
    if(this.endsWith(str))
        end = length - str.length;
    if(start == 0 && end == length)
        return this;
    return this.substring(start, end);
}

/**
 * 去掉两头的字符
 * @param preffix 头部要去掉的子字符串
 * @param suffix 尾部要去掉的子字符串
 * @return
 */
public inline fun String.trim(preffix:String, suffix:String): String {
    var start = 0;
    var end = length
    if(this.startsWith(preffix))
        start = preffix.length;
    if(this.endsWith(suffix))
        end = length - suffix.length;
    if(start == 0 && end == length)
        return this;
    if(start >= end)
        return "";
    return this.substring(start, end);
}

/**
 * 替换字符串
 *
 * @param params 参数
 * @return
 */
public inline fun String.replaces(params:List<String>):String
{
    return this.replace(":(\\d+)".toRegex()){ matches:MatchResult ->
        val i = matches.groupValues[1]
        val value = params.get(i.toInt());
        value.toString()
    };
}

/**
 * 替换字符串
 *
 * @param params 参数
 * @return
 */
public inline fun String.replaces(params:Map<String, Any?>):String
{
    return this.replace(":([\\w\\d]+)".toRegex()){ matches:MatchResult ->
        val i = matches.groupValues[1]
        val value = params.get(i);
        if(value == null)
            ""
        else
            value.toString()
    };
}

/**
 * 下划线转驼峰
 * @return
 */
public fun String.underline2Camel(): String {
    return "_\\w".toRegex().replace(this){ result: MatchResult ->
        // 获得下划线后的字母
        val ch = result.value[1]
        // 转为大写
        Character.toUpperCase(ch).toString()
    }
}

/**
 * 驼峰转下划线
 * @return
 */
public fun String.camel2Underline(): String {
    return "[A-Z]".toRegex().replace(this){ result: MatchResult ->
        // 获得大写字母
        val ch = result.value
        // 转为下划线+小写
        "_" + ch.toLowerCase()
    }
}

/****************************** 字符串转化其他类型 *******************************/
/**
 * 日期格式
 */
val dateFormat = SimpleDateFormat("yyyy-MM-dd")
/**
 * 日期-时间格式
 */
val datetimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

/**
 * 转换为日期类型
 * @return
 */
public fun String.toDate(): Date {
    val format: SimpleDateFormat = if(contains(':')) datetimeFormat else dateFormat;
    return format.parse(this)
}

/**
 * 将字符串转换为指定类型的可空值
 * @param class 要转换的类型
 * @param defaultValue 默认值
 * @return
 */
public inline fun <T: Any> String?.toNullable(clazz: KClass<T>, defaultValue: T? = null): T?{
    // 默认值
    if(this == null)
        return defaultValue

    // 转换
    return to(clazz)
}

/**
 * 将字符串转换为指定类型的非空值
 * @param class 要转换的类型
 * @return
 */
public inline fun <T: Any> String.to(clazz: KClass<T>): T{
    // 1 如果要转换为 String类及其父类，则直接返回，不用转换
    if(clazz.java.isAssignableFrom(String::class.java))
        return this as T

    // 2 转换为其他类型
    var result: Any?;
    when(clazz){
        Int::class -> result = this.toInt()
        Long::class -> result = this.toLong()
        Float::class -> result = this.toFloat()
        Double::class -> result = this.toDouble()
        Boolean::class -> result = this.toBoolean()
        Short::class -> result = this.toShort()
        Byte::class -> result = this.toByte()
        Date::class -> result = this.toDate()
        else -> throw IllegalArgumentException("字符串不能自动转换为未识别的类型: " + clazz)
    }
    return result as T;
}

/**
 * 将字符串转换为指定类型的非空值
 * @param type
 * @return
 */
public inline fun String.to(type: KType): Any{
    return this.to(type.classifier as KClass<*>)
}