package com.jkmvc.db

import java.util.*
import kotlin.jvm.internal.FunctionImpl

/**
 * sql修饰子句的模拟构建
 *     每个修饰子句(如where xxx and yyy/group by xxx, yyy)包含多个子表达式(如where可以有多个条件子表达式, 如name="shi", age=1), 每个子表达式有多个元素组成(如name/=/"shi")
 *     每个元素有对应的处理函数
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-13
 *
 */
abstract class DbQueryBuilderDecorationClauses<T>(protected val operator: String /* 修饰符， 如where/group by */, protected val elementHandlers: Array<FunctionImpl?> /* 每个元素的处理器, 可视为列的处理*/)
: IDbQueryBuilderDecorationClauses<T> {
    /**
     * 子表达式, 可视为行
     * @var array
     */
    protected val subexps: MutableList<T> by lazy {
        LinkedList<T>();
    };

    /**
     * 编译多个子表达式
     * @return string
     */
    public override fun compile(): String {
        if (subexps.isEmpty())
            return ""; // toString() 必须返回字符串，不能返回null

        // 逐个子表达式编译+合并
        val sql: StringBuilder = StringBuilder(operator).append(' ');
        subexps.forEach {
            compileSubexp(it, sql);
        }

        return sql.toString();
    }

    /**
     * 清空
     * @return DbQueryBuilderDecorationClauses
     */
    public override fun clear(): IDbQueryBuilderDecorationClauses<T> {
        subexps.clear();
        return this;
    }

    /**
     * 转换字符串时, 直接编译
     * @return string
     */
    public override fun toString(): String {
        return compile ();
    }

}