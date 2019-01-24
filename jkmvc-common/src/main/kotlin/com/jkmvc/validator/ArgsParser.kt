package com.jkmvc.validator

import com.jkmvc.common.exprTo
import com.jkmvc.common.mapIndexedToArray
import java.lang.reflect.Method
import java.util.ArrayList
import kotlin.reflect.KFunction

/**
 * 函数参数解析
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-24 10:24 AM
 */
object ArgsParser {

    /**
     * 单个参数的正则
     */
    //private val REGEX_ARG: Regex = ("([\\w\\d-: \"\\u4e00-\\u9fa5]+),?").toRegex();
    private val REGEX_ARG: Regex = ("([^,]+),?").toRegex();

    /**
     * 解析函数实参, 不解析具体类型
     *
     * @param exp 参数表达式, 包含多个参数, 参数之间用,分隔, 如 `("hello", 1)` 或 `"hello", 1`
     *            括号可有可无
     *            每个参数不能包含以下字符: `,()`
     * @return
     */
    public fun parse(exp: String): Array<String> {
        val matches: Sequence<MatchResult> = REGEX_ARG.findAll(exp);
        val result: ArrayList<String> = ArrayList();
        for(m in matches)
            result.add(m.groups[1]!!.value)

        //return result.toArray() as Array<String> // java.lang.ClassCastException: [Ljava.lang.Object; cannot be cast to [Ljava.lang.String;
        return Array<String>(result.size){i ->
            result[i]
        }
    }

    /**
     * 解析函数实参, 根据方法形参类型来解析具体类型
     *
     * @param exp 参数表达式, 包含多个参数, 参数之间用,分隔
     * @param method 方法
     * @return
     */
    public fun parse(exp: String, method: Method): Array<Any?> {
        val args = parse(exp)
        return args.mapIndexedToArray { i, arg ->
            arg.exprTo(method.parameterTypes[i])
        }
    }

    /**
     * 解析函数实参, 根据方法形参类型来解析具体类型
     *
     * @param exp 参数表达式, 包含多个参数, 参数之间用,分隔
     * @param fuc 方法
     * @return
     */
    public fun parse(exp: String, fuc: KFunction<*>): Array<Any?> {
        val args = parse(exp)
        return args.mapIndexedToArray { i, arg ->
            arg.exprTo(fuc.parameters[i].type)
        }
    }

}