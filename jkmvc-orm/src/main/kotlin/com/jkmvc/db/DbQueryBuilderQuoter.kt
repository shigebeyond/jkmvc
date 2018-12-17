package com.jkmvc.db

/**
 * 表名/字段名/值的转移器
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 11:43 AM
 */
abstract class DbQueryBuilderQuoter: IDbQueryBuilder(){
    /**
     * 缓存编译好的sql
     */
    protected var compiledSql: CompiledSql = CompiledSql();

    /**
     * 清空条件
     * @return
     */
    public override fun clear(): IDbQueryBuilder {
        compiledSql.clear();
        return this;
    }

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone() as DbQueryBuilder
        // 复制编译结果
        o.compiledSql = compiledSql.clone() as CompiledSql
        return o;
    }

    /**
     * 转移值
     *   转义为?, 并搜集sql参数
     * @param db
     * @param value
     * @return
     */
    public override fun quote(db: IDb, value: Any?): String {
        // sql参数化: 将参数名拼接到sql, 独立出参数值, 以便执行时绑定参数值
        // 1 null => "NULL" -- oracle中不能使用null作为参数，因此只能直接输出null作为sql
        if (value == null)
            return "NULL";

        // 2 子查询: 编译select子句 + 并合并参数到 compiledSql 中
        if(value is IDbQueryBuilder)
            return quoteSubQuery(db, value)

        // 3 db表达式
        if(value is DbExpr && value !== DbExpr.question) {
            if(value.exp is IDbQueryBuilder)
                return quoteSubQuery(db, value.exp, value.alias)

            return value.toString()
        }

        // 4 字段值
        compiledSql.staticParams.add(value);
        return "?";
    }

    /**
     * 转义子查询
     *   编译select子句 + 合并参数到 compiledSql 中
     * @param db
     * @param subquery
     * @param alias
     * @return
     */
    public override fun quoteSubQuery(db: IDb, subquery: IDbQueryBuilder, alias: String?): String {
        val subsql = subquery.compileSelect()
        compiledSql.staticParams.addAll(subsql.staticParams);
        if(alias == null)
            return "(" + subsql.sql + ")"

        return "(${subsql.sql}) ${db.quoteIdentifier(alias)}"
    }


}