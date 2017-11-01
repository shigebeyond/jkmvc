package com.jkmvc.db


/**
 * 分组子句
 * 	子句中有子句, 而第二层的子句由分组来管理
 *     一个分组有多个子句, 就是使用()来包含的子句
 *
 *  实现:
 *     子表达式还是子句
 *
 * @author shijianhang
 * @date 2016-10-13
 */
class DbQueryBuilderDecorationClausesGroup(operator: String /* 修饰符， 如where/group by */, elementHandlers: Array<((Any?) -> String)?> /* 每个元素的处理器, 可视为列的处理*/)
: DbQueryBuilderDecorationClauses<Any>/* subexps 是字符串 或 DbQueryBuilderDecorationClausesSimple */(operator, elementHandlers) {
    /**
     * 开启一个分组
     *
     * @param  delimiter
     * @return
     */
    public override fun open(delimiter: String): IDbQueryBuilderDecorationClauses<Any> {
        // 将连接符也记录到子表达式中, 忽略第一个子表达式
        val exp = if(subexps.isEmpty() || subexps.last == "(")  // "("表示子表达式的开始
                    "("
                  else
                    " $delimiter ("
        subexps.add(exp)
        return this;
    }

    /**
     * 结束一个分组
     *
     * @return
     */
    public override fun close(): IDbQueryBuilderDecorationClauses<Any> {
        subexps.add(")")
        return this;
    }

    /**
     * 获得最后一个子表达式
     *
     * @return
     */
    protected fun endSubexp(): DbQueryBuilderDecorationClausesSimple {
        var last:Any? = if(subexps.isEmpty()) null else subexps.last()
        if (last !is DbQueryBuilderDecorationClausesSimple) {
            val afterGroup = last == ")" // 跟在分组后面
            last = DbQueryBuilderDecorationClausesSimple("", elementHandlers, afterGroup);
            subexps.add(last);
        }

        return last;
    }

    /**
     * 添加子表达式
     *
     * @param subexp 子表达式
     * @param delimiter 连接符
     * @return
     */
    public override fun addSubexp(subexp: Array<Any?>, delimiter: String): IDbQueryBuilderDecorationClauses<Any> {
        // 代理最后一个子表达式
        endSubexp().addSubexp(subexp, delimiter);
        return this;
    }

    /**
     * 编译一个子表达式
     *
     * @param subexp 子表达式
     * @param j 索引
     * @param sql 保存编译的sql
     */
    public override fun compileSubexp(subexp: Any, j:Int, sql: StringBuilder): Unit {
        // 子表达式是: string / DbQueryBuilderDecorationClausesSimple
        if (subexp is DbQueryBuilderDecorationClausesSimple) {
            subexp.compile(sql);
        }else{
            sql.append(subexp);
        }
    }

}