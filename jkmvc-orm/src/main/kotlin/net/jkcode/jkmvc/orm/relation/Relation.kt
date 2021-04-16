package net.jkcode.jkmvc.orm.relation

import net.jkcode.jkmvc.orm.*
import net.jkcode.jkmvc.query.IDbQueryBuilder
import net.jkcode.jkutil.common.dbLogger
import java.util.*
import kotlin.reflect.KClass

/**
 * 关联关系
 *
 * 涉及到2类查询
 *    1 通过join语句来联查 A/B 两表
 *      如 A/B 表是一对一的关系，在查询 A 表记录时，可以通过 join B 表来联查，具体实现参考 IRelation.applyQueryJoinRelated()
 *    2 根据 A 表记录来查 B 表记录
 *      如 A 表先查出来，再根据 A 表来查 B 表，一般用在 OrmRelated.related() 与 hasMany关系的联查（不能使用join在同一条sql中查询），具体实现参考 RelationMeta.queryRelated()
 *
 * 关于 cascadeDeleted:
 *      只对 hasOne/hasMany 有效, 对 belongsTo/hasOneThrough/hasManyThrough 无效
 *      对 belongsTo, 你敢删除 belongsTo 关系的主对象？
 *      对 hasOneThrough/hasManyThrough, 都通过中间表来关联了, 两者之间肯定是独立维护的, 只删除关联关系就好, 不删除关联对象
 */
abstract class Relation(
        public override val one2one: Boolean, // 是否一对一, 否则一对多
        public override val srcOrmMeta: IOrmMeta, // 源模型元数据
        public override val model: KClass<out IOrm>, // 关联模型类型
        public override val foreignKey: DbKeyNames, // 外键
        public override val primaryKey: DbKeyNames, // 主键
        public override val conditions:RelationConditions = RelationConditions.EmptyConditions, // 查询条件
        public override val cascadeDeleted: Boolean = false, // 是否级联删除
        public override val pkEmptyRule: PkEmptyRule = model.modelOrmMeta.pkEmptyRule // 检查主键为空的规则
) : IRelation {

    /**
     * 是否是`有一个`关系
     *    当前表是主表, 关联表是从表
     */
    public override val isBelongsTo: Boolean
        get() = this is BelongsToRelation

    /**
     * 是否是`从属于`关系
     *    当前表是从表, 关联表是主表
     */
    public override val isHasOne: Boolean
        get() = this !is BelongsToRelation && this.one2one

    /**
     * 是否是`有多个`关系
     * 	当前表是主表, 关联表是从表
     */
    public override val isHasMany: Boolean
        get() = this !is BelongsToRelation && !this.one2one

    /**
     * 关系名
     */
    public override lateinit var name: String

    /**
     * 主键属性
     *   与 primaryKey 对应
     */
    public override val primaryProp: DbKeyNames = srcOrmMeta.columns2Props(primaryKey)

    /**
     *  外键属性
     *   与 foreignKey 对应
     */
    public override val foreignProp: DbKeyNames = srcOrmMeta.columns2Props(foreignKey)

    /**
     * 外键对应字段的默认值
     */
    override val foreignKeyDefault: DbKeyValues
        get(){
            val slave = slaveOrmMeta as OrmMeta
            return foreignKey.map { col ->
                slave.dbColumns[col]?.default
            }
        }

    /**
     * 通过编译好的sql, 来延迟查询关联对象
     * @param 当前对象
     * @return 关联对象, 可能多个
     */
    override fun lazySelectRelatedByCompiledSql(thisItem: IOrm): Any? {
        val key = thisItem.gets(thisProp).toList()
        return if (isHasMany) // 查多个
                    lazySelectRelatedSql.findRows(key, ormMeta.db, modelRowTransformer)
                else  // 查一个
                    lazySelectRelatedSql.findRow(key, ormMeta.db, modelRowTransformer)
    }

    /**
     * 检查主键外键是否存在
     */
    override fun checkKeyExist() {
        // 检查主键是否存在
        val masterTable = masterOrmMeta.dbTable
        if (!masterTable.hasColumns(primaryKey))
            throw OrmException("Master table [${masterTable.name}] miss `primaryKey` columns: $primaryKey")

        // 检查外键是否存在
        val slaveTable = slaveOrmMeta.dbTable
        if (!slaveTable.hasColumns(foreignKey))
            throw OrmException("Slave table [${slaveTable.name}] miss `foreignKey` columns: $foreignKey")
    }

    /**
     * 批量设置关系的属性值, 即将关联模型对象 塞到 本模型对象的属性
     *
     * @param items 本模型对象
     * @param relatedItems 关联模型对象
     */
    public override fun batchSetRelationProp(items: List<IOrm>, relatedItems: List<IOrm>) {
        // 设置关联属性为空值, 否则会延迟加载查sql
        if(relatedItems.isEmpty()){
            for (item in items)
                item[name] = emptyValue
            return
        }

        // 检查主外键的类型: 数据库中主外键字段类型可能不同，则无法匹配
        val firstTk: DbKeyValues = items.first().gets(thisProp)
        val firstRk: DbKeyValues = relatedItems.first().gets(relatedProp)
        firstTk.forEachColumnWith(firstRk){ pk, fk, i ->
            if (pk != null && fk != null && pk::class != fk::class)
                //throw OrmException("模型[${ormMeta.name}]联查[${name}]关联对象失败: 本表键[${ormMeta.table}.${thisProp}]字段类型[${firstTk::class}]与关联表键[${this.model.modelOrmMeta.table}.${relationProp}]字段类型[${firstRk::class}]不一致，请改成一样的")
                throw OrmException("Fail to query model [${ormMeta.name}]'s related object [${name}]: this table key [${ormMeta.table}.${thisProp}]'s class [${firstTk::class}], mismatch the related table key [${this.model.modelOrmMeta.table}.${relatedProp}]'s class [${firstRk::class}]")
        }

        // 设置关联属性 -- 双循环匹配主外键
        for (item in items) { // 遍历每个源对象，收集关联对象
            val tk: DbKeyValues = item.gets(thisProp) // 本表键
            var match = false
            for (relatedItem in relatedItems) { // 遍历每个关联对象，进行匹配
                // 关系的匹配： 本表键=关联表键
                val rk: DbKeyValues = relatedItem.gets(relatedProp) // 关联表键
                if (tk.equals(rk)) { // DbKey.equals()
                    match = true
                    if(this.isHasMany){ // hasMany关联对象是list
                        val myRelated = (item as OrmRelated).getOrPutList(name)
                        myRelated.add(relatedItem)
                    }else{ // 一对一关联对象是单个对象
                        item[name] = relatedItem
                    }
                }
            }

            // 没有匹配则给个空list
            if(!match)
                item[name] = emptyList<Any?>()
        }

        // 清空列表
        (relatedItems as MutableList).clear()
    }

    /**
     * 删除关联对象, 级联删除的源头
     *
     * @param item 本模型对象
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    override fun deleteRelated(item: IOrm, fkInMany: Any?): Boolean{
        // 构建关联查询：自动构建查询条件
        val relatedQuery = queryRelated(item, fkInMany)
        if(relatedQuery == null)
            return true

        // 删过的模型: 因为是级联删除的源头, 因此记录源模型
        val srcModel = srcOrmMeta.name
        val deletedModels = mutableSetOf(srcModel)
        // 当前关系的路径
        val path = Stack<String>()
        path.add(srcModel)

        // 级联删除
        val ret = deleteRelated(relatedQuery, deletedModels, path)
        deletedModels.clear()
        return ret
    }

    /**
     * 根据关联对象的查询, 来删除关联对象
     *    级联删除: 组织->部门->雇员
     * @param relatedQuery 关联对象的查询
     * @param deletedModels 记录删除过的模型
     * @param path 当前关系的路径
     * @return
     */
    protected fun deleteRelated(relatedQuery: IDbQueryBuilder, deletedModels: MutableSet<String>, path: Stack<String>): Boolean{
        // 去重: 已删除过
        // model名, 如果有中间表, 则用中间表做前缀
        var model = if(this is HasNThroughRelation)
                        middleTable + ":" + ormMeta.name
                    else
                        ormMeta.name
        val separator = "->"
        if(deletedModels.contains(model)) {
            if(dbLogger.isDebugEnabled)
                dbLogger.debug("Delete duplicated model [{}] when deleting related path: {}", model, path.joinToString(separator) + separator + model)
            return true
        }

        deletedModels.add(model) // 记录删过模型
        path.add(name) // 关系入栈

        if(dbLogger.isDebugEnabled)
            dbLogger.debug("Deleting related path's next related: {}", path.joinToString(separator))

        // 1 递归删除下一层
        for(relation in ormMeta.hasNOrThroughRelations as List<Relation>){
            if(relation.cascadeDeleted) // 级联删除: 递归删除下一层关联对象
                relation.deleteRelated(relatedQuery, deletedModels, path)
            else { // 仅删除下一层关系
                if(dbLogger.isDebugEnabled)
                    dbLogger.debug("Deleting relation path: {}", path.joinToString(separator) + separator + relation.name)
                relation.removeRelation(relatedQuery)
            }
        }

        // 2 删除当前层关联对象
        if(dbLogger.isDebugEnabled)
            dbLogger.debug("Deleting related path: {}", path.joinToString(separator))
        val ret = doDeleteRelated(relatedQuery)
        path.pop() // 关系出栈
        return ret
    }

    /**
     * 删除关系（删除从表的外键值）
     *
     * @param @param subquery 当前对象的查询
     * @return
     */
    protected abstract fun removeRelation(subquery: IDbQueryBuilder): Boolean

    /**
     * 删除当前层关联对象
     *
     * @param relatedQuery 关联对象子查询
     * @return
     */
    protected abstract fun doDeleteRelated(relatedQuery: IDbQueryBuilder): Boolean
}