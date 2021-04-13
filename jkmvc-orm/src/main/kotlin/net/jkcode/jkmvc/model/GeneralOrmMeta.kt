package net.jkcode.jkmvc.model

import net.jkcode.jkmvc.orm.*
import kotlin.reflect.KClass

/**
 * 通用模型的元数据
 *    改写 queryBuilder(), 返回 GeneralOrmQueryBuilder
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 3:38 PM
 */
open class GeneralOrmMeta(label: String, // 模型中文名
                           table: String, // 表名
                           primaryKey:DbKeyNames = DbKeyNames("id"), // 主键
                           cacheMeta: OrmCacheMeta? = null, // 缓存配置
                           dbName: String = "default" // 数据库名
): OrmMeta(GeneralModel::class, label, table, primaryKey, cacheMeta, dbName){

    public constructor(
            label: String, // 模型中文名
            table: String, // 表名，假定model类名, 都是以"Model"作为后缀
            primaryKey: String, // 主键
            cacheMeta: OrmCacheMeta? = null, // 缓存配置
            dbName: String = "default" // 数据库名
    ) : this(label, table, DbKeyNames(primaryKey), cacheMeta, dbName)

    /**
     * 改写 queryBuilder(), 返回 GeneralOrmQueryBuilder
     *
     * @param convertingValue 查询时是否智能转换字段值
     * @param convertingColumn 查询时是否智能转换字段名
     * @param withSelect with()联查时自动select关联表的字段
     * @param reused 是否复用的 -- 暂不支持
     * @return
     */
    public override fun queryBuilder(convertingValue: Boolean, convertingColumn: Boolean, withSelect: Boolean, reused: Boolean): OrmQueryBuilder {
        return GeneralOrmQueryBuilder(this, convertingValue, convertingColumn, withSelect);
    }
}