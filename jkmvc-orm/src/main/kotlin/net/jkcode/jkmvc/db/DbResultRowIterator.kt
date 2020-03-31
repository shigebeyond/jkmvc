package net.jkcode.jkmvc.db

/**
 * 结果集一行的值迭代器
 */
class DbResultRowIterator(public val row: DbResultRow): Iterator<Pair<String, Any?>> {

    /**
     * 当前序号
     *   rs.getObject(i)获取列值时，下标是从“1”开始
     */
    protected var _curr = 0

    override fun hasNext(): Boolean {
        return _curr < row.rs.columnCount
    }

    override fun next(): Pair<String, Any?> {
        val rs = row.rs
        // rs.getObject(i)获取列值时，下标是从“1”开始
        if(_curr++ >= rs.columnCount)
            throw IndexOutOfBoundsException("超过列数")

        val label: String = rs.metaData.getColumnLabel(_curr); // 字段名
        val value: Any? = rs.getValue(_curr) // 字段值
        return label to value

    }

}