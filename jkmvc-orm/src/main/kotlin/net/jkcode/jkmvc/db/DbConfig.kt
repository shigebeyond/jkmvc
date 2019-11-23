package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.Config

/**
 * db配置
 * @author shijianhang<772910474@qq.com>
 * @date 2019-08-23 5:16 PM
 */
object DbConfig {
    /**
     * 公共配置
     */
    public val config: Config = Config.instance("db", "yaml")

    /**
     * 是否调试
     */
    public val debug: Boolean = config.getBoolean("debug", false)!!;

    /**
     * 分库的数据库名
     */
    public val shardingDbs: List<String> = config.getString("shardingDbs", "")!!.split(',');

    /**
     * 是否分库
     */
    public fun isSharding(db: String): Boolean {
        return shardingDbs.contains(db)
    }

    /**
     * 字段有下划线
     */
    public val columnUnderlineDbs: List<String> = config.get("columnUnderlineDbs", "")!!.split(',')

    /**
     * 是否字段有下划线
     */
    public fun isColumnUnderline(db: String): Boolean {
        return columnUnderlineDbs.contains(db)
    }

    /**
     * 字段全大写
     */
    public val columnUpperCaseDbs: List<String> = config.get("columnUpperCaseDbs", "")!!.split(',')

    /**
     * 是否字段全大写
     */
    public fun isColumnUpperCase(db: String): Boolean {
        return columnUpperCaseDbs.contains(db)
    }
}