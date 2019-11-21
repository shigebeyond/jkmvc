package net.jkcode.jkmvc.db

import java.sql.ResultSet

/**
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-11-21 4:27 PM
 */
class DbResultSet(public val rs: ResultSet) {

    public inline operator fun <reified T> get(column: String): T?{
        rs.getObject(column)
        return null
    }
}