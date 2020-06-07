package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.replacesFormat

/**
 * 列
 *   参考 hibernate 的 org.hibernate.mapping.Column
 *
 * @author shijianhang
 * @date 2020-2-4 下午8:02:47
 */
data class DbColumn(
        public val name: String, // 列名
        public val logicType: DbColumnLogicType, // 逻辑类型
        public val physicalType: String? = null, // 物理类型
        public var length: Int? = null, // 长度, 一般是指字符串长度, 也用作数字长度
        public val scale: Int? = null, // 精度
        public val default: String? = null, // 默认值, 转义后的值
        public val nullable: Boolean = true, // 是否可为null
        public val comment: String? = null, // 注释
        public val autoIncr: Boolean = false, // 是否自增
        public val table: String = "" // 表名
) {

    init {
        if(length == null && logicType == DbColumnLogicType.VARCHAR && physicalType.isNullOrBlank())
            length = 255
    }

    /**
     * 数字长度, 直接用length
     */
    public val precision: Int?
        get() = length

    /**
     * 是否不为null
     */
    public val notNullable: Boolean
        get() = !nullable

    /**
     * 转map
     */
    public fun toMap(db: IDb): Map<String, String?> {
        // 真正的物理类型
        val physicalType = if(this.physicalType.isNullOrBlank())
                                logicType.toPhysicalType(db, precision, scale)
                            else
                                physicalType
        return mapOf(
                "name" to db.quoteColumn(name),
                "type" to physicalType,
                "default" to default,
                "comment" to comment,
                // bool类型, true才输出, false则输出null
                // 因为如果参数值为null, 才不会格式化输出
                "nullable" to if(nullable) "true" else null,
                "notNullable" to if(notNullable) "true" else null,
                "autoIncr" to if(autoIncr) "true" else null
        )
    }

    /**
     * 生成字段定义sql
     * @param db
     * @return
     */
    public fun generateDefineColumnSql(db: IDb): String {
        // 元数据定义的配置
        val config = Config.instance("db-meta.${db.dbType}", "yaml")
        // 字段sql
        val columnSql: String = config["columnSql"]!!

        // 替换参数: 如果参数值为null, 才不会格式化输出
        return columnSql.replacesFormat(toMap(db), "<", ">")
    }

    override fun equals(other: Any?): Boolean {
        return other is DbColumn && this.name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}