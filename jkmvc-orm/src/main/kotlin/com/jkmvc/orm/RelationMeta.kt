package com.jkmvc.orm

import kotlin.reflect.KClass

/**
 * 关联关系的元数据
 *
 * 涉及到2类查询
 *    1 通过join语句来联查 A/B 两表
 *      如 A/B 表是一对一的关系，在查询 A 表记录时，可以通过 join B 表来联查，具体实现参考 OrmQueryBuilder.joinSlave() / joinMaster()
 *    2 根据 A 表记录来查 B 表记录
 *      如 A 表先查出来，再根据 A 表来查 B 表，一般用在 OrmRelated.related() 与 hasMany关系的联查（不能使用join在同一条sql中查询），具体实现参考 RelationMeta.queryRelated()
 */
open class RelationMeta(
        public override val sourceMeta:IOrmMeta, /* 源模型元数据 */
        public override val type:RelationType /* 关联关系 */,
        public override val model: KClass<out IOrm> /* 关联模型类型 */,
        public override val foreignKey:String /* 外键 */,
        public override val primaryKey:String/* 主键 */,
        public override val conditions:Map<String, Any?> = emptyMap() /* 查询条件 */
): IRelationMeta {

    /**
     * 主键属性
     */
    public override val primaryProp:String = sourceMeta.column2Prop(primaryKey)

    /**
     *  外键属性
     */
    public override val foreignProp:String = sourceMeta.column2Prop(foreignKey)

    /**
     * 构造函数
     * @param sourceMeta 源模型元数据
     * @param type 关联关系
     * @param model 关联模型类型
     * @param foreignKey 外键
     * @param conditions 查询条件
     */
    /*public constructor(sourceMeta:IOrmMeta, type:RelationType, model: KClass<out IOrm>, foreignKey:String, conditions:Map<String, Any?> = emptyMap()):this(sourceMeta, type, model, foreignKey, sourceMeta.primaryKey, conditions){
    }*/

    /**
     * 查询关联表
     *     自动根据关联关系，来构建查询条件
     *
     * @param item Orm对象
     * @param withTableAlias 是否带表前缀
     * @return
     */
    public override fun queryRelated(item: IOrm, withTableAlias:Boolean): OrmQueryBuilder {
        val query = queryBuilder()
        val tableAlias = if(withTableAlias)
                            model.modelName + '.'
                        else
                            ""
        if(type == RelationType.BELONGS_TO) { // 查主表
            query.where(tableAlias + primaryKey, "=", item[foreignProp]) // 主表.主键 = 从表.外键
        } else { // 查从表
            query.where(tableAlias + foreignKey, "=", item[primaryProp]) // 从表.外键 = 主表.主键
        }
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
        if(type == RelationType.BELONGS_TO) { // 查主表
            query.where(model.modelName + '.' + primaryKey, "IN", items.collectColumn(foreignProp)) // 主表.主键 = 从表.外键
        } else { // 查从表
            query.where(model.modelName + '.' + foreignKey, "IN", items.collectColumn(primaryProp)) // 从表.外键 = 主表.主键
        }
        return query
    }

}