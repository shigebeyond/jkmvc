package com.jkmvc.db

/**
 * 简单的(sql修饰)子句
 *
 * 	在子表达式的拼接中，何时拼接连接符？
 *     1 在compile()时拼接连接符 => 你需要单独保存每个子表达式对应的连接符，在拼接时取出
 *     2 在addSubexp()就将连接符也记录到子表达式中 => 在compile()时直接连接子表达式的内容就行，不需要关心连接符的特殊处理
 *     我采用的是第二种
 *
 * @author shijianhang
 * @date 2016-10-13
 */
class DbQueryBuilderDecorationClausesSimple(operator: String /* 修饰符， 如where/group by */, elementHandlers: Array<((Any?) -> String)?> /* 每个元素的处理器, 可视为列的处理*/, protected val afterGroup:Boolean = false /* 跟在分组 DbQueryBuilderDecorationClausesGroup 后面 */)
: DbQueryBuilderDecorationClauses<Pair<Array<Any?>, String>>/* subexps 是子表达式+连接符 */(operator, elementHandlers) {
    /**
     * 添加一个子表达式+连接符
     *
     * @param subexp 子表达式
     * @param delimiter 当前子表达式的连接符
     * @return
     */
    public override fun addSubexp(subexp: Array<Any?>, delimiter: String): IDbQueryBuilderDecorationClauses<Pair<Array<Any?>, String>> {
        // 将连接符也记录到子表达式中, 忽略第一个子表达式的连接符 => 编译好子表达式直接拼接就行
        subexps.add(Pair(subexp, delimiter));
        return this;
    }

    /**
     * 编译一个子表达式
     *
     * @param subexp 子表达式
     * @param j 索引
     * @param sql 保存编译的sql
     */
    public override fun compileSubexp(subexp: Pair<Array<Any?>, String>, j:Int, sql: StringBuilder): Unit {
        val (exp, delimiter) = subexp;

        // 跟在分组后面的第一个元素要在前面插入连接符
        if(afterGroup && j == 0)
            sql.append(delimiter).append(' ');

        // 遍历处理器来处理对应元素, 没有处理的元素也直接拼接
        for (i in elementHandlers.indices) {
            val handler: ((Any?) -> String)? = elementHandlers[i];
            // 处理某个元素的值
            var value: Any? = exp[i];
            if (exp.size > i && handler != null) {
                value = handler.invoke(exp[i]); // 调用元素处理函数
            }
            sql.append(value).append(' '); // // 用空格拼接多个元素
        }

        // 非最后一个元素要在后面追加连接符
        if(j != subexps.size - 1)
            sql.append(delimiter).append(' ');
    }

    /**
     * 开启一个分组
     *
     * @param delimiter 连接符
     * @return
     */
    public override fun open(delimiter: String): IDbQueryBuilderDecorationClauses<Pair<Array<Any?>, String>> {
        throw UnsupportedOperationException("not implemented")
    }

    /**
     * 结束一个分组
     *
     * @return
     */
    public override fun close(): IDbQueryBuilderDecorationClauses<Pair<Array<Any?>, String>> {
        throw UnsupportedOperationException("not implemented")
    }
}