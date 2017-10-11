package com.jkmvc.orm

import kotlin.reflect.KClass

/**
 * 有中间表的关联关系的元数据
 *
 * 特征
 *   1 用中间表来存储两表的关联关系
 *   2 两表对彼此都是hasMany的关联关系
 *
 * 只涉及到1类查询
 *   hasMany关系的关联查询
 *   => 不用考虑一对一关系(belongsTo / hasOne)
 */
class MiddleRelationMeta(
        sourceMeta:IOrmMeta, /* 源模型元数据 */
        type:RelationType /* 关联关系 */,
        model: KClass<out IOrm> /* 关联模型类型 */,
        foreignKey:String /* 外键 */,
        primaryKey:String/* 主键 */,
        public val middleTable:String/* 中间表 */,
        public val farForeignKey:String /* 远端外键 */,
        public val farPrimaryKey:String/* 远端主键 */
) : RelationMeta(sourceMeta, type, model, foreignKey, primaryKey) {

    /**
     * 远端主键属性
     */
    public val farPrimaryProp:String = sourceMeta.column2Prop(farPrimaryKey)

    /**
     *  远端外键属性
     */
    public val farForeignProp:String = sourceMeta.column2Prop(farForeignKey)

    /**
     * 通过中间表来查询从表
     * @return
     */
    protected fun queryMiddleRelated(): OrmQueryBuilder {
        // 通过join中间表 查从表
        return queryBuilder()
                .select(model.modelName + ".*", middleTable + '.' + foreignKey) // 查字段：中间表.外键 = 主表.主键，以便在查询后绑定主对象
                .join(middleTable).on(middleTable + '.' + farForeignKey, "=", model.modelName + '.' + farPrimaryKey) as OrmQueryBuilder // 中间表.远端外键 = 从表.远端主键
    }

    /**
     * 查询从表
     *     根据hasMany的关联关系，来构建查询条件
     *
     * @param item Orm对象
     * @return
     */
    public override fun queryRelated(item: IOrm): OrmQueryBuilder {
        // 通过join中间表 查从表
        return queryMiddleRelated()
                .where(middleTable + '.' + foreignKey, "=", item[primaryProp]) as OrmQueryBuilder // 中间表.外键 = 主表.主键
    }

    /**
     * 查询从表
     *     根据hasMany的关联关系，来构建查询条件
     *
     * @param items Orm列表
     * @return
     */
    public override fun queryRelated(items: Collection<out IOrm>): OrmQueryBuilder {
        // 通过join中间表 查从表
        return queryMiddleRelated()
                .where(middleTable + '.' + foreignKey,  "IN", items.collectColumn(primaryProp)) as OrmQueryBuilder // 中间表.外键 = 主表.主键
    }
}
