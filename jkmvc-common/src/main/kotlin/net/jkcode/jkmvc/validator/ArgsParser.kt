package net.jkcode.jkmvc.validator

import net.jkcode.jkmvc.common.exprTo
import net.jkcode.jkmvc.common.mapIndexedToArray
import net.jkcode.jkmvc.common.trim
import java.lang.reflect.Method
import java.util.*
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
     * @param exp 参数表达式, 是以()包围多个参数, 参数之间用`,`分隔, 如 `("hello", 1)` 或 `"hello", 1`
     *            每个参数不能包含以下字符: `,()`
     * @return
     */
    public fun parse(exp: String): Array<String> {
        val exp2 = exp.trim("(", ")") // 去括号
        if(exp2.isBlank())
            return emptyArray()

        val matches: Sequence<MatchResult> = REGEX_ARG.findAll(exp2);
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
     * @param exp 参数表达式, 是以()包围多个参数, 参数之间用`,`分隔
     * @param method 方法
     * @param hasBrackets 是否包含括号
     * @return
     */
    public fun parse(exp: String, method: Method): Array<Any?> {
        val args = parse(exp)
        if(args.size != method.parameters.size)
            throw IllegalArgumentException("参数表达式中的参数个数=${args.size}, 不等于方法的参数个数=${method.parameters.size}")
        return args.mapIndexedToArray { i, arg ->
            arg.exprTo(method.parameterTypes[i])
        }
    }

    /**
     * 解析函数实参, 根据方法形参类型来解析具体类型
     *
     * @param exp 参数表达式, 是以()包围多个参数, 参数之间用`,`分隔
     * @param fuc 方法
     * @param hasBrackets 是否包含括号
     * @return
     */
    public fun parse(exp: String, fuc: KFunction<*>): Array<Any?> {
        val args = parse(exp)
        return args.mapIndexedToArray { i, arg ->
            arg.exprTo(fuc.parameters[i].type)
        }
    }

}