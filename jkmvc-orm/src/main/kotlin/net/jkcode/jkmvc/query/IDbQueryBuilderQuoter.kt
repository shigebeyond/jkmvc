package net.jkcode.jkmvc.query

import net.jkcode.jkutil.common.iteratorArrayOrCollection
import net.jkcode.jkutil.common.joinToString
import net.jkcode.jkmvc.db.IDb

/**
 * 表名/字段名/值的转移器
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 11:43 AM
 */
interface IDbQueryBuilderQuoter {

    /**
     * 转义值
     *
     * @param db
     * @param value 字段值, 可以是值数组
     * @return
     */
    fun quote(db: IDb, value: Any?): String{
        // 1 多值
        val itr = value?.iteratorArrayOrCollection()
        if(itr != null){
            return itr.joinToString(", ", "(", ")") {
                quoteSingleValue(db, it)
            }
        }

        // 2 两值, 一般是 where between 的值
        if (value is Pair<*, *>)
            return quoteSingleValue(db, value.first) + " AND " + quoteSingleValue(db, value.second)

        // 3 单值
        return quoteSingleValue(db, value)
    }

    /**
     * 转义单个值
     *
     * @param db
     * @param value 字段值
     * @return
     */
    fun quoteSingleValue(db: IDb, value: Any?): String

    /**
     * 转义字段名
     *
     * @param db
     * @param table
     * @return
     */
    fun quoteColumn(db: IDb, column: CharSequence): String{
        return db.quoteColumn(column)
    }

    /**
     * 转义表名
     *
     * @param db
     * @param table
     * @return
     */
    fun quoteTable(db: IDb, table: CharSequence): String{
        // 1 子查询
        if(table is DbExpr && table.exp is IDbQueryBuilder)
            return quoteSubQuery(db, table.exp, table.alias)

        if(table is IDbQueryBuilder)
            return quoteSubQuery(db, table)

        // 2 普通表
        return db.quoteTable(table)
    }

    /**
     * 转义子查询
     *
     * @param db
     * @param subquery
     * @param alias
     * @return
     */
    fun quoteSubQuery(db: IDb, subquery: IDbQueryBuilder, alias: String? = null): String
}