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
internal open class GeneralOrmMeta(model: KClass<out IOrm>, // 模型类
                                   label: String = model.modelName, // 模型中文名
                                   table: String = model.modelName, // 表名，假定model类名, 都是以"Model"作为后缀
                                   primaryKey:DbKeyNames = DbKeyNames("id"), // 主键
                                   cached: Boolean = false, // 是否缓存
                                   dbName: String = "default" // 数据库名
): OrmMeta(model, label, table, primaryKey, cached, dbName){

    public constructor(
            model: KClass<out IOrm>, // 模型类
            label: String, // 模型中文名
            table: String, // 表名，假定model类名, 都是以"Model"作为后缀
            primaryKey: String, // 主键
            cached: Boolean = false, // 是否缓存
            dbName: String = "default" // 数据库名
    ) : this(model, label, table, DbKeyNames(primaryKey), cached, dbName)

    /**
     * 改写 queryBuilder(), 返回 GeneralOrmQueryBuilder
     *
     * @param convertingValue 查询时是否智能转换字段值
     * @param convertingColumn 查询时是否智能转换字段名
     * @param withSelect with()联查时自动select关联表的字段
     * @return
     */
    public override fun queryBuilder(convertingValue: Boolean, convertingColumn: Boolean, withSelect: Boolean): OrmQueryBuilder {
        return GeneralOrmQueryBuilder(this, convertingValue, convertingColumn, withSelect);
    }
}