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
data class MiddleRelationMeta(
        public override val sourceMeta:IOrmMeta, /* 源模型元数据 */
        public override val type:RelationType /* 关联关系 */,
        public override val model: KClass<out IOrm> /* 关联模型类型 */,
        public override val foreignKey:String /* 外键 */,
        public override val primaryKey:String/* 主键 */,
        public val middleTable:String/* 中间表 */,
        public val farForeignKey:String /* 远端外键 */,
        public val farPrimaryKey:String/* 远端主键 */
) : RelationMeta(sourceMeta, type, model, foreignKey, primaryKey) {

    /**
     * 查询关联表
     *     根据hasMany的关联关系，来构建查询条件
     *
     * @param item Orm对象
     * @return
     */
    public override fun queryRelated(item: IOrm): OrmQueryBuilder {
        val query = queryBuilder()
        // 查从表
        query.where(middleTable + '.' + foreignKey, "=", item[primaryProp]) // 中间表.外键 = 主表.主键
        query.join(middleTable).on(middleTable + '.' + farForeignKey, "=", model.modelName + '.' + farPrimaryKey) // 从表.远端外键 = 远端主表.远端主键
        return query
    }

    /**
     * 查询关联表
     *     自动根据关联关系，来构建查询条件
     *
     * @param items Orm列表
     * @return
     */
    public override fun queryRelated(items: Collection<out IOrm>): OrmQueryBuilder {
        val query = queryBuilder()
        // 查从表
        query.where(foreignKey, "IN", items.collectColumn(primaryProp)) // 从表.外键 = 主表.主键
        return query
    }
}
