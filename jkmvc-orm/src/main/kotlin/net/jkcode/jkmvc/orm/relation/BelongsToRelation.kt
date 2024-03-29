package net.jkcode.jkmvc.orm.relation

import net.jkcode.jkmvc.orm.*
import net.jkcode.jkmvc.orm.DbKeyValues
import net.jkcode.jkmvc.query.CompiledSql
import net.jkcode.jkmvc.query.DbExpr
import net.jkcode.jkmvc.query.IDbQueryBuilder
import kotlin.reflect.KClass

/**
 * 从属于的关联关系
 *
 */
class BelongsToRelation(
        srcOrmMeta: IOrmMeta, // 源模型元数据
        model: KClass<out IOrm>, // 关联模型类型
        foreignKey: DbKeyNames, // 外键
        primaryKey: DbKeyNames, // 主键
        conditions:RelationConditions = RelationConditions.EmptyConditions, // 查询条件
        pkEmptyRule: PkEmptyRule = model.modelOrmMeta.pkEmptyRule // 检查主键为空的规则
) : Relation(true, srcOrmMeta, model, foreignKey, primaryKey, conditions, false, pkEmptyRule) {

    /**
     * 本模型不是主表, 是从表
     */
    override val thisAsMaster: Boolean = false

    /**
     * 延迟查询关联对象的sql
     */
    override val lazySelectRelatedSql: CompiledSql by lazy{
        val fk = foreignProp.map {
            DbExpr.question
        }
        queryBuilder().where(primaryKey.wrap(modelName + ".") /*modelName + "." + primaryKey*/, fk) // 主表.主键 = 从表.外键
                .limit(1)
                .compileSelect()
    }

    /**
     * 查询关联表
     *     自动根据关联关系，来构建查询条件
     *     查主表
     *     对BelongsTo关系，如果外键为空，则联查为空
     *
     * @param item (从表)当前Orm对象
     * @param fkInMany 无用, hasMany关系下的单个外键值Any|对象IOrm，如果为null，则更新所有关系, 否则更新单个关系
     * @return
     */
    public override fun queryRelated(item: IOrm, fkInMany: Any?): OrmQueryBuilder? {
        // 查主表
        val fk: DbKeyValues = item.gets(foreignProp)
        if(pkEmptyRule.isEmpty(fk)) // 如果外键为空，则联查为空
            return null

        return queryBuilder().where(primaryKey.wrap(modelName + ".") /*modelName + "." + primaryKey*/, fk) as OrmQueryBuilder // 主表.主键 = 从表.外键
    }

    /**
     * 查询关联表
     *     自动根据关联关系，来构建查询条件
     *     查主表
     *
     * @param items (从表)当前Orm列表
     * @return
     */
    public override fun queryRelated(items: Collection<out IOrm>): OrmQueryBuilder? {
        if(items.isEmpty())
            return null

        // 查主表
        return queryBuilder().whereIn(primaryKey.wrap(modelName + ".") /*modelName + "." + primaryKey*/, items.collectColumns(foreignProp)) as OrmQueryBuilder // 主表.主键 = 从表.外键
    }

    /**
     * 查询关联表
     *    自动根据关联关系，来构建查询条件
     *    查主表
     *
     * @param subquery (从表)当前子查询
     * @return
     */
    override fun queryRelated(subquery: IDbQueryBuilder): OrmQueryBuilder{
        // 查主表
        val subqueryAlias = "sub_" + subquery.tableAlias
        return queryBuilder()
                .join(DbExpr(subquery.copy(true).select(foreignKey.wrap(subquery.tableAlias + ".") /* TODO: 加子查询内的表前缀 */), subqueryAlias), "INNER")
                .on(primaryKey.wrap(modelName + ".") /*modelName + "." + primaryKey*/, foreignKey.wrap(subqueryAlias + ".") /*subqueryAlias + foreignKey*/) as OrmQueryBuilder // 主表.主键 = 从表.外键
    }

    /**
     * 对query builder联查关联表(主表)
     *   主表.主键 = 从表.外键
     *
     * @param query
     * @param thisName 当前表别名 = 从表别名
     * @param relatedName 关联表别名 = 主表别名
     * @return
     */
    override fun applyQueryJoinRelated(query: OrmQueryBuilder, thisName:String, relatedName: String){
        val slave = ormMeta
        val slaveName = thisName
        val masterRelation = this
        val masterName = relatedName

        // 检查并添加联查关系
        if(!query.addJoinOne(masterName))
            return

        // 准备条件
        val slaveFk:DbKeyNames = masterRelation.foreignKey.map { // 从表.外键
            slaveName + "." + it // slaveName + "." + masterRelation.foreignKey
        }

        val master: IOrmMeta = masterRelation.ormMeta;
        val masterPk:DbKeyNames = masterRelation.primaryKey.map {  // 主表.主键
            masterName + "." + it //masterName + "." + masterRelation.primaryKey
        }

        // 查主表
        query.join(DbExpr(master.table, masterName), "LEFT").on(masterPk, slaveFk) // 主表.主键 = 从表.外键
    }

    /**
     * 添加关系（添加从表的外键值）
     *
     * @param item (从表)本模型对象
     * @param value (主表)外键值Any | 关联对象IOrm
     * @return
     */
    override fun addRelation(item: IOrm, value: Any): Boolean{
        return setRelation(item, primaryProp.getsFrom(value))
    }

    /**
     * 设置关系（设置从表的外键值）
     *
     * @param item (从表)本模型对象
     * @param value (主表)外键值Any | 关联对象IOrm, 若为null则表示删除
     * @return
     */
    protected fun setRelation(item: IOrm, value: Any?): Boolean {
        item.sets(foreignProp, value)
        return item.update()
    }

    /**
     * 删除关系（删除从表的外键值）
     *
     * @param item (从表)本模型对象
     * @param fkInMany 无用, hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    override fun removeRelation(item: IOrm, fkInMany: Any?): Boolean{
        return setRelation(item, foreignKeyDefault)
    }

    /**
     * 删除关系（删除从表的外键值）
     *
     * @param @param subquery (从表)当前对象的查询
     * @return
     */
    override fun removeRelation(subquery: IDbQueryBuilder): Boolean{
        // 清空 从表.外键
        return subquery.set(foreignKey.wrap(subquery.tableAlias + "."), foreignKeyDefault) // 清空外键
                .update()
    }

    /**
     * 删除当前层关联对象
     *    你敢删除 belongsTo 关系的主对象？
     *
     * @param relatedQuery (主表)关联对象子查询
     * @return
     */
    protected override fun doDeleteRelated(relatedQuery: IDbQueryBuilder): Boolean {
        throw OrmException("Cannot delete model [${ormMeta.name}] 's `belongsTo` related object [$name]");
    }

}
