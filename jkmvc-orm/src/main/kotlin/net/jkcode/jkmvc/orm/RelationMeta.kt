package net.jkcode.jkmvc.orm

import java.util.*
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
        public override val cascadeDeleted: Boolean = false, // 是否级联删除
        public override val pkEmptyRule: PkEmptyRule = model.modelOrmMeta.pkEmptyRule // 检查主键为空的规则
) : IRelationMeta {

    /**
     * 关系名
     */
    public override lateinit var name: String

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
            if(pkEmptyRule.isEmpty(fk)) // 如果外键为空，则联查为空
                return null
            return queryBuilder().where(primaryKey.wrap(tableAlias) /*tableAlias + primaryKey*/, fk) as OrmQueryBuilder // 主表.主键 = 从表.外键
        }

        // 查从表
        val pk:DbKeyValues = item.gets(primaryProp) // 主键
        if(item.isPkEmpty(pk))
            return null
        val query = queryBuilder().where(foreignKey.wrap(tableAlias) /*tableAlias + foreignKey*/, pk) as OrmQueryBuilder// 从表.外键 = 主表.主键
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
            query.whereIn(primaryKey.wrap(model.modelName + '.') /*model.modelName + '.' + primaryKey*/, items.collectColumn(foreignProp)) // 主表.主键 = 从表.外键
        } else { // 查从表
            query.whereIn(foreignKey.wrap(model.modelName + '.') /*model.modelName + '.' + foreignKey*/, items.collectColumn(primaryProp)) // 从表.外键 = 主表.主键
        }
        return query
    }

    /**
     * 设置关系的属性值, 即将关联模型对象 塞到 本模型对象的属性
     *
     * @param items 本模型对象
     * @param relatedItems 关联模型对象
     */
    public override fun setRelationProp(items: List<IOrm>, relatedItems: List<IOrm>) {
        if(items.isEmpty())
            return

        // 设置关联属性为空list, 否则会延迟加载查sql
        if(relatedItems.isEmpty()){
            for (item in items)
                item[name] = emptyList<Any>()
            return
        }

        // 获得本表键与关联表键
        val thisProp:DbKeyNames
        val relationProp:DbKeyNames
        if(type == RelationType.BELONGS_TO){
            thisProp = this.foreignProp // 从表.外键
            relationProp = this.primaryProp // 主表.主键
        }else {
            thisProp = this.primaryProp // 主表.主键
            relationProp = if (this is MiddleRelationMeta)
                                this.middleForeignProp // 中间表.外键
                            else
                                this.foreignProp // 从表.外键
        }

        // 检查主外键的类型: 数据库中主外键字段类型可能不同，则无法匹配
        val firstTk:DbKeyValues = items.first().gets(thisProp)
        val firstRk:DbKeyValues = relatedItems.first().gets(relationProp)
        firstTk.forEachColumnWith(firstRk){ pk, fk, i ->
            if (pk != null && fk != null && pk::class != fk::class)
                throw OrmException("模型[${ormMeta.name}]联查[${name}]关联对象失败: 本表键[${ormMeta.table}.${thisProp}]字段类型[${firstTk::class}]与关联表键[${this.model.modelOrmMeta.table}.${relationProp}]字段类型[${firstRk::class}]不一致，请改成一样的")
        }

        // 设置关联属性 -- 双循环匹配主外键
        for (item in items) { // 遍历每个源对象，收集关联对象
            for (relatedItem in relatedItems) { // 遍历每个关联对象，进行匹配
                // 关系的匹配： 本表键=关联表键
                val tk:DbKeyValues = item.gets(thisProp) // 本表键
                val rk:DbKeyValues = relatedItem.gets(relationProp) // 关联表键
                if (tk.equals(rk)) { // DbKey.equals()
                    if(type == RelationType.HAS_MANY){ // hasMany关联对象是list
                        val myRelated = item.getOrPut(name){
                            ArrayList<IOrm>()
                        } as MutableList<IOrm>
                        myRelated.add(relatedItem)
                    }else{ // 其他关联对象是单个对象
                        item[name] = relatedItem
                    }

                }
            }
        }

        // 清空列表
        (relatedItems as MutableList).clear()
    }

}