package net.jkcode.jkmvc.orm.relation

import net.jkcode.jkmvc.orm.*
import net.jkcode.jkmvc.orm.DbKeyValues
import net.jkcode.jkmvc.query.DbExpr
import kotlin.reflect.KClass

/**
 * 从属于的关联关系
 *
 */
open class HasNRelation(
        one2one: Boolean, // 是否一对一, 否则一对多
        srcOrmMeta: IOrmMeta, // 源模型元数据
        model: KClass<out IOrm>, // 关联模型类型
        foreignKey: DbKeyNames, // 外键
        primaryKey: DbKeyNames, // 主键
        conditions:Map<String, Any?> = emptyMap(), // 查询条件
        cascadeDeleted: Boolean = false, // 是否级联删除
        pkEmptyRule: PkEmptyRule = model.modelOrmMeta.pkEmptyRule // 检查主键为空的规则
) : Relation(one2one, srcOrmMeta, model, foreignKey, primaryKey, conditions, cascadeDeleted, pkEmptyRule) {

    /**
     * 空值
     */
    override val emptyValue: Any?
        get() = if(one2one) null else emptyList<Any>()

    /**
     * 本模型键属性
     */
    override val thisProp: DbKeyNames
        get() = primaryProp // 主表.主键

    /**
     *  关联模型键属性
     */
    override val relatedProp: DbKeyNames
        get() = foreignProp // 从表.外键

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
        query.whereIn(foreignKey.wrap(model.modelName + '.') /*model.modelName + '.' + foreignKey*/, items.collectColumn(primaryProp)) // 从表.外键 = 主表.主键
        return query
    }

    /**
     * 查询关联表
     *     自动根据关联关系，来构建查询条件
     *     查从表
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

        // 查从表
        val pk: DbKeyValues = item.gets(primaryProp) // 主键
        if(item.isPkEmpty(pk))
            return null
        val query = queryBuilder().where(foreignKey.wrap(tableAlias) /*tableAlias + foreignKey*/, pk) as OrmQueryBuilder// 从表.外键 = 主表.主键
        if(fkInMany != null) { // hasMany关系下过滤单个关系
            val fk = ormMeta.primaryProp.getsFrom(fkInMany)
            query.where(tableAlias + ormMeta.primaryKey, fk)
        }
        return query;
    }

    /**
     * 对query builder联查关联表(从表)
     *    从表.外键 = 主表.主键
     *
     * @param query
     * @param thisName 当前表别名 = 主表别名
     * @param relatedName 关联表别名 = 从表别名
     * @return
     */
    override fun applyQueryJoinRelated(query: OrmQueryBuilder, thisName:String, relatedName: String){
        val master = ormMeta
        val masterName = thisName
        val slaveRelation = this
        val slaveName = relatedName

        // 检查并添加联查关系
        if(!query.addJoinOne(slaveName))
            return

        // 准备条件
        val masterPk:DbKeyNames = slaveRelation.primaryKey.map{  // 主表.主键
            masterName + "." + it // masterName + "." + slaveRelation.primaryKey
        };

        val slave = slaveRelation.ormMeta
        val slaveFk:DbKeyNames = slaveRelation.foreignKey.map {  // 从表.外键
            slaveName + "." + it // slaveName + "." + slaveRelation.foreignKey
        }

        // 查从表
        query.join(DbExpr(slave.table, slaveName), "LEFT").on(slaveFk, masterPk) // 从表.外键 = 主表.主键
    }

    /**
     * 添加关系（添加从表的外键值）
     *
     * @param item (主表)本模型对象
     * @param value (从表)外键值Any | 关联对象IOrm
     * @return
     */
    override fun addRelation(item: IOrm, value: Any): Boolean{
        // 更新关联对象的外键
        // 1 orm对象直接改外键值
        if(value is IOrm){
            //value[foreignProp] = item[primaryProp]
            value.sets(foreignProp, item.gets(primaryProp))
            return value.update()
        }

        // 2 手动改外键值
        val query = queryBuilder()
        val relatedPrimaryKey = ormMeta.primaryKey // 关联对象的主键字段
        if(value is Collection<*>) { // 多个
            query.whereIn(relatedPrimaryKey, (value as Collection<out IOrm>).collectColumn(relatedPrimaryKey))
        }else{ // 单个
            val relatedPk = relatedPrimaryKey.getsFrom(value)
            query.where(relatedPrimaryKey, relatedPk)
        }
        // 更新关联对象的外键字段
        return query.set(foreignKey, item.gets(primaryProp)).update()
    }

    /**
     * 添加关系（删除从表的外键值）
     *
     * @param item
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    override fun removeRelation(item: IOrm, fkInMany: Any?): Boolean{
        // 手动改外键值为null
        val nullValue = foreignKey.map { null }
        return queryRelated(item, fkInMany)!!.set(foreignKey, nullValue).update()
    }
}
