package net.jkcode.jkmvc.db

/**
 * 结果集的迭代器
 */
class DbResultSetIterator(public val rs: DbResultSet): Iterator<DbResultRow> {

    override fun hasNext(): Boolean {
        return rs.next()
    }

    override fun next(): DbResultRow {
        return DbResultRow(rs)
    }

}