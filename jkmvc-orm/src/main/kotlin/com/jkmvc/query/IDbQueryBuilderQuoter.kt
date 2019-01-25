package com.jkmvc.query

import com.jkmvc.common.iteratorArrayOrCollection
import com.jkmvc.common.joinToString
import com.jkmvc.db.IDb

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
                quote(db, it)
            }
        }

        // 2 单值
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