package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.replacesFormat
import java.util.LinkedHashMap

/**
 * 表
 *   参考 hibernate 的 org.hibernate.mapping.Table
 *
 * @author shijianhang
 * @date 2020-2-4 下午8:02:47
 */
class DbTable(
    public val name: String, // 表名
    public val catalog: String? = null,
    public val schema: String? = null
) {

    /**
     * 列
     */
    public val columns: Map<String, DbColumn> = LinkedHashMap()

    /**
     * 主键
     */
    public var primaryKeys: List<String>? = null

    /**
     * 添加字段
     * @param column
     */
    public fun addClumn(column: DbColumn) {
        (columns as MutableMap)[column.name] = column
    }

    /**
     * 根据字段名获得字段
     * @param name
     * @return
     */
    public fun getColumn(name: String): DbColumn? {
        return columns[name]
    }

    /**
     * 生成建表sql
     * @param db
     * @return
     */
    public fun generateCreateTableSql(db: Db): String {
        // 元数据定义的配置
        val config = Config.instance("meta-define.${db.dbType}", "yaml")
        // 建表语句
        val tableSql: String = config["createTableSql"]!!
        // 生成字段定义sql
        val columnsSql = columns.values.joinToString(",\n\t") {
            it.generateDefineColumnSql(db)
        }

        val data = mapOf(
                "table" to db.quoteTable(name),
                "columns" to columnsSql,
                "primaryKeys" to primaryKeys?.joinToString {
                    db.quoteColumn(it)
                }
        )
        return tableSql.replacesFormat(data, "<", ">")
                .replace("\\n", "\n")
                .replace("\\t", "\t")

    }

}