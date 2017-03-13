package com.jkmvc.db

import kotlin.jvm.internal.FunctionImpl

/**
 * 分组子句
 * 	子句中有子句, 而第二层的子句由分组来管理
 *     一个分组有多个子句, 就是使用()来包含的子句
 *
 *  实现:
 *     子表达式还是子句
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-13
 *
 */
class DbQueryBuilderDecorationClausesGroup(operator: String /* 修饰符， 如where/group by */, elementHandlers: Array<FunctionImpl?> /* 每个元素的处理器, 可视为列的处理*/)
: DbQueryBuilderDecorationClauses<Any>/* subexps 是字符串 或 DbQueryBuilderDecorationClausesSimple */(operator, elementHandlers) {
    /**
     * 开启一个分组
     *
     * @param    delimiter
     * @return DbQueryBuilderDecorationClausesGroup
     */
    public override fun open(delimiter: String): IDbQueryBuilderDecorationClauses<Any> {
        // 将连接符也记录到子表达式中, 忽略第一个子表达式
        subexps.add("(")
        return this;
    }

    /**
     * 结束一个分组
     *
     * @return DbQueryBuilderDecorationClausesGroup
     */
    public override fun close(): IDbQueryBuilderDecorationClauses<Any> {
        subexps.add(")")
        return this;
    }

    /**
     * 获得最后一个子表达式
     *
     * @return DbQueryBuilderDecorationClausesSimple
     */
    public fun endSubexp(): DbQueryBuilderDecorationClausesSimple {
        var last = subexps.last()
        if (last !is DbQueryBuilderDecorationClausesSimple) {
            last = DbQueryBuilderDecorationClausesSimple("", elementHandlers);
            subexps.add(last);
        }

        return last as DbQueryBuilderDecorationClausesSimple;
    }

    /**
     * 添加子表达式
     *
     * @param array subexp
     * @param string delimiter
     * @return DbQueryBuilderDecorationClausesGroup
     */
    public override fun addSubexp(subexp: Array<Any?>, delimiter: String): IDbQueryBuilderDecorationClauses<Any> {
        // 代理最后一个子表达式
        endSubexp().addSubexp(subexp, delimiter);
        return this;
    }

    /**
     * 编译一个子表达式
     *
     * @param array subexp
     * @return
     */
    public override fun compileSubexp(subexp: Any, sql: StringBuilder): Unit {
        // 子表达式是: string / DbQueryBuilderDecorationClausesSimple
        // DbQueryBuilderDecorationClausesSimple 转字符串自动compile
        sql.append(subexp);
    }

}