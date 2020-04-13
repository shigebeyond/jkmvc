package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.dbLogger
import net.jkcode.jkutil.common.replacesFormat
import java.util.LinkedHashMap

/**
 * 表
 *   1 参考 hibernate 的 org.hibernate.mapping.Table
 *   2 支持生成建表/改表/删表sql
 *   3 改表sql: 目前只支持新建列, 对已有列不做修改/删除, 防止不小心丢了数据无法恢复
 *
 * @author shijianhang
 * @date 2020-2-4 下午8:02:47
 */
open class DbTable(
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
    public var primaryKeys: Collection<String> = emptyList()

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
     * 更新表结构
     * @param db
     */
    public fun updateSchema(db: IDb){
        // 1 如果表存在则改表
        if(db.queryTableExist(name)){
            val sqls = generateAlterTableSqls(db)
            for (sql in sqls) {
                dbLogger.info("改表[{}]: {}", name, sql)
                db.execute(sql)
            }
            return
        }

        // 2 否则建表
        val sql = generateCreateTableSql(db)
        dbLogger.info("建表[{}]: {}", name, sql)
        db.execute(sql)
    }

    /**
     * 删除表
     * @param db
     */
    public fun dropSchema(db: IDb){
        val sql = generateDropTableSql(db)
        db.execute(sql)
    }

    /**
     * 生成建表sql
     * @param db
     * @return
     */
    public fun generateCreateTableSql(db: IDb): String {
        // 元数据定义的配置
        val config = Config.instance("db-meta.${db.dbType}", "yaml")
        // 建表sql
        val createTableSql: String = config["createTableSql"]!!
        // 生成字段定义sql
        val columnsSql = columns.values.joinToString(",\n\t") {
            it.generateDefineColumnSql(db)
        }

        // 参数
        val data = mapOf(
                "table" to db.quoteTable(name),
                "columnsSql" to columnsSql,
                "primaryKeys" to primaryKeys?.joinToString {
                    db.quoteColumn(it)
                }
        )
        // 生成sql
        return createTableSql.replacesFormat(data, "<", ">")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
    }

    /**
     * 生成改表sql
     *    目前只支持新建列, 对已有列不做修改/删除, 防止不小心丢了数据无法恢复
     *
     * @param db
     * @return
     */
    public fun generateAlterTableSqls(db: IDb): List<String>{
        // 元数据定义的配置
        val config = Config.instance("db-meta.${db.dbType}", "yaml")
        // 建表sql
        val createTableSql: String = config["alterTableSql"]!!

        // 获得旧的列
        val oldColumns = db.queryColumnsByTable(name).map { it.name }
        
        // 对比获得新的列
        val newColumns = columns.keys.subtract(oldColumns)

        // 遍历每个新的列, 生成添加列的sql
        return newColumns.map {
            val columnSql = columns[it]!!.generateDefineColumnSql(db)

            // 参数
            val data = mapOf(
                    "table" to db.quoteTable(name),
                    "columnSql" to columnSql,
                    "primaryKeys" to primaryKeys?.joinToString {
                        db.quoteColumn(it)
                    }
            )
            // 生成sql
            createTableSql.replacesFormat(data, "<", ">")
        }

    }

    /**
     * 生成删表sql
     * @param db
     * @return
     */
    public fun generateDropTableSql(db: IDb): String {
        // 元数据定义的配置
        val config = Config.instance("db-meta.${db.dbType}", "yaml")
        // 建表sql
        val dropTableSql: String = config["createTableSql"]!!

        // 参数
        val data = mapOf(
                "table" to db.quoteTable(name)
        )
        // 生成sql
        return dropTableSql.replacesFormat(data, "<", ">")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
    }


}