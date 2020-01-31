package net.jkcode.jkmvc.orm

import kotlin.reflect.KClass

/**
 * 关联关系的元数据
 *
 * 涉及到2类查询
 *    1 通过join语句来联查 A/B 两表
 *      如 A/B 表是一对一的关系，在查询 A 表记录时，可以通过 join B 表来联查，具体实现参考 OrmQueryBuilder.joinSlave() / joinMaster()
 *    2 根据 A 表记录来查 B 表记录
 *      如 A 表先查出来，再根据 A 表来查 B 表，一般用在 OrmRelated.related() 与 hasMany关系的联查（不能使用join在同一条sql中查询），具体实现参考 RelationMeta.queryRelated()
 *
 * 关于 cascadeDeleted:
 *      只对 hasOne/hasMany 有效, 对 belongsTo/hasOneThrough/hasManyThrough 无效
 *      对 belongsTo, 你敢删除 belongsTo 关系的主对象？
 *      对 hasOneThrough/hasManyThrough, 都通过中间表来关联了, 两者之间肯定是独立维护的, 只删除关联关系就好, 不删除关联对象
 */
open class RelationMeta(
        public override val sourceMeta:IOrmMeta, // 源模型元数据
        public override val type:RelationType, // 关联关系
        public override val model: KClass<out IOrm>, // 关联模型类型
        public override val foreignKey:DbKeyNames, // 外键
        public override val primaryKey:DbKeyNames, // 主键
        public override val conditions:Map<String, Any?> = emptyMap(), // 查询条件
        public override val cascadeDeleted: Boolean = false // 是否级联删除
) : IRelationMeta {

    /**
     * 约定int类型的外键必须是正整数
     * TODO: 可配置
     */
    public val intForeighKeyMustPositive:Boolean = true

    /**
     * 约定string类型的外键必须不为空
     * TODO: 可配置
     */
    public val stringForeighKeyMustNotEmpty:Boolean = true

    /**
     * 主键属性
     *   与 primaryKey 对应
     */
    public override val primaryProp:DbKeyNames = sourceMeta.columns2Props(primaryKey)

    /**
     *  外键属性
     *   与 foreignKey 对应
     */
    public override val foreignProp:DbKeyNames = sourceMeta.columns2Props(foreignKey)

    /**
     * 检查指定外键值是否为空
     *
     * @param fk 外键值
     * @return
     */
    public override fun isForeighKeyEmpty(fk: Any?): Boolean {
        return fk == null
                || (fk is Int && intForeighKeyMustPositive && fk == 0)
                || (fk is String && stringForeighKeyMustNotEmpty && fk.isEmpty())
    }

    /**
     * 查询关联表
     *     自动根据关联关系，来构建查询条件
     *     对belongs_to关系，如果外键为空，则联查为空
     *
     * @param item Orm对象
     * @param fkInMany hasMany关系下的单个外键值Any|对象IOrm，如果为null，则更新所有关系, 否则更新单个关系
     * @param withTableAlias 是否带表前缀
     * @return
     */
    public override fun queryRelated(item: IOrm, fkInMany: Any?, withTableAlias:Boolean): OrmQueryBuilder? {
        val tableAlias = if(withTableAlias)
                            model.modelName + '.'
                        else
                            ""
        // 查主表
        if(type == RelationType.BELONGS_TO) {
            val fk:DbKeyValues = item.gets(foreignProp)
            if(isForeighKeysAllEmpty(fk)) // 如果外键为空，则联查为空
                return null
            return queryBuilder().where(primaryKey.wrap(tableAlias) /*tableAlias + primaryKey*/, "=", fk) as OrmQueryBuilder // 主表.主键 = 从表.外键
        }

        // 查从表
        val pk:DbKeyValues = item.gets(primaryProp) // 主键
        if(pk.isAnyNull())
            return null
        val query = queryBuilder().where(foreignKey.wrap(tableAlias) /*tableAlias + foreignKey*/, "=", pk) as OrmQueryBuilder// 从表.外键 = 主表.主键
        if(fkInMany != null) { // hasMany关系下过滤单个关系
            val fk = if(fkInMany is IOrm) fkInMany.gets(ormMeta.primaryProp) else fkInMany
            query.where(tableAlias + ormMeta.primaryKey, fk)
        }
        return query;
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
            query.where(primaryKey.wrap(model.modelName + '.') /*model.modelName + '.' + primaryKey*/, "IN", items.collectColumn(foreignProp)) // 主表.主键 = 从表.外键
        } else { // 查从表
            query.where(foreignKey.wrap(model.modelName + '.') /*model.modelName + '.' + foreignKey*/, "IN", items.collectColumn(primaryProp)) // 从表.外键 = 主表.主键
        }
        return query
    }

}