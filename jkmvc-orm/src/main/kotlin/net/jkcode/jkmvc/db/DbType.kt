package net.jkcode.jkmvc.db

/**
 * 数据库类型
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
public enum class DbType {
    // 名字长的放前面，以便根据driverClass来获得db类型时，能更好的匹配
    Postgresql,
    SqlServer,
    Oracle,
    Hsqldb,
    Sqlite,
    Ingres,
    Mysql,
    DB2,
    H2
}