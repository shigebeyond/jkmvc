package com.jkmvc.query

import com.jkmvc.db.DbException

/**
 * 要插入的数据
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 11:43 AM
 */
class InsertData: Cloneable{
    /**
     * 要插入的字段
     */
    public var columns:Array<String> = emptyArray();

    /**
     * 要插入的多行数据，但是只有一维，需要按columns的大小，来拆分成多行
     */
    public var rows: ArrayList<Any?> = ArrayList();

    /**
     * 检查行的大小
     * @param rowSize
     * @return
     */
    protected fun checkRowSize(rowSize:Int){
        if(isSubQuery())
            throw DbException("已插入子查询，不能再插入值");

        if(columns.isEmpty())
            throw DbException("请先调用insertColumn()来设置插入的字段名");

        // 字段值数，是字段名数的整数倍
        val columnSize = columns.size
        if(rowSize % columnSize != 0)
            throw IllegalArgumentException("请插入的字段值数[$rowSize]与字段名数[$columnSize]不匹配");
    }

    /**
     * 是否插入子查询
     * @return
     */
    public fun isSubQuery(): Boolean {
        return rows.isNotEmpty() && rows[0] is IDbQueryBuilder
    }

    /**
     * 获得子查询
     * @return
     */
    public fun getSubQuery(): IDbQueryBuilder {
        return rows[0] as IDbQueryBuilder
    }

    /**
     * 添加一行/多行
     * @param row
     * @return
     */
    public fun add(row: Array<out Any?>): InsertData {
        checkRowSize(row.size)
        rows.addAll(row)
        return this;
    }

    /**
     * 添加子查询
     * @param subquery
     * @return
     */
    public fun add(subquery: IDbQueryBuilder): InsertData {
        if(rows.isNotEmpty())
            throw DbException("已插入其他值，不能再插入子查询");

        rows.add(subquery)
        return this;
    }

    /**
     * 添加一行/多行
     */
    public fun add(row: Collection<Any?>): InsertData {
        checkRowSize(row.size)
        rows.addAll(row)
        return this;
    }

    /**
     * 清空数据
     */
    public fun clear() {
        columns = emptyArray();
        rows.clear();
    }

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone() as InsertData
        o.columns = columns.clone()
        o.rows = rows.clone() as ArrayList<Any?>
        return o;
    }
}