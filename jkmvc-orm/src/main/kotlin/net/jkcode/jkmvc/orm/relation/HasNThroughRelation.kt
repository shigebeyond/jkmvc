package net.jkcode.jkmvc.orm.relation

import net.jkcode.jkmvc.orm.*
import net.jkcode.jkmvc.orm.DbKeyValues
import net.jkcode.jkmvc.query.DbExpr
import net.jkcode.jkmvc.query.DbQueryBuilder
import net.jkcode.jkmvc.query.IDbQueryBuilder
import kotlin.reflect.KClass

/**
 * 有中间表的关联关系
 *
 * 特征
 *   1 用中间表来存储两表的关联关系
 *   2 两表对彼此都是hasMany/hasOne的关联关系
 *
 * 表结构是2个字段
 *   1 foreignKey： 中间表.外键 = 主表.主键
 *   2 farForeignKey： 中间表.远端外键 = 从表.远端主键
 *
 * 只涉及到2类的关联查询
 *   只针对hasMany/hasOne
 *   不考虑belongsTo
 *
 * 关于 cascadeDeleted:
 *      只对 hasOne/hasMany 有效, 对 belongsTo/hasOneThrough/hasManyThrough 无效
 *      对 belongsTo, 你敢删除 belongsTo 关系的主对象？
 *      对 hasOneThrough/hasManyThrough, 都通过中间表来关联了, 两者之间肯定是独立维护的, 只删除关联关系就好, 不删除关联对象
 */
class HasNThroughRelation(
        one2one: Boolean, // 是否一对一, 否则一对多
        srcOrmMeta: IOrmMeta, // 源模型元数据
        model:KClass<out IOrm>, // 关联模型类型
        foreignKey: DbKeyNames, // 外键
        primaryKey: DbKeyNames, // 主键
        public val middleTable:String, // 中间表
        public val farForeignKey: DbKeyNames, // 远端外键
        public val farPrimaryKey: DbKeyNames, // 远端主键
        conditions:Map<String, Any?> = emptyMap(), // 查询条件
        pkEmptyRule: PkEmptyRule = model.modelOrmMeta.pkEmptyRule // 检查主键为空的规则
) : Relation(one2one, srcOrmMeta, model, foreignKey, primaryKey, conditions, false, pkEmptyRule) {

    /**
     * 本模型作为主表
     */
    override val thisAsMaster: Boolean = true

    /**
     *  关联模型键属性
     */
    override val relatedProp: DbKeyNames
        get() = middleForeignProp // 中间表.外键

    /**
     * 远端主键属性
     *   与 farPrimaryKey 对应
     */
    public val farPrimaryProp: DbKeyNames = srcOrmMeta.columns2Props(farPrimaryKey)

    /**
     *  远端外键属性
     *    与 farForeignKey 对应
     */
    public val farForeignProp: DbKeyNames = srcOrmMeta.columns2Props(farForeignKey)

    /**
     * 中间表的外键字段别名
     *    用在 OrmQueryBuilder.findRows() 联查从表时，绑定主对象
     *    不能使用foreignKey, 因为中间表的该字段可能与从表字段重名
     */
    public val middleForeignKey: DbKeyNames = foreignKey.wrap("", "_") // foreignKey + '_'

    /**
     * 中间表的外键属性
     *    与 middleForeignKey 对应
     */
    public val middleForeignProp: DbKeyNames = srcOrmMeta.columns2Props(middleForeignKey)

    /**
     * 构建查询：通过join中间表来查询从表
     * @return
     */
    protected fun buildQuery(): OrmQueryBuilder {
        // select关联字段：中间表.外键 = 主表.主键，用在 OrmQueryBuilder.findRows() 联查从表时，绑定主对象
         //val smfk = DbExpr(middleTable + '.' + foreignKey, middleForeignKey)
        val smfk: DbKey<DbExpr> = foreignKey.mapWith(middleForeignKey){ fk, mfk ->
            DbExpr(middleTable + '.' + fk, mfk)
        }
        return queryBuilder()
                .select(model.modelName + ".*", *smfk.columns)
                .join(middleTable).on(farForeignKey.wrap(middleTable + '.') /*middleTable + '.' + farForeignKey*/,  farPrimaryKey.wrap(model.modelName + '.') /*model.modelName + '.' + farPrimaryKey*/) as OrmQueryBuilder // 中间表.远端外键 = 从表.远端主键
    }

    /**
     * 查询中间表
     *
     * @return
     */
    public fun queryMiddleTable(): IDbQueryBuilder {
        return DbQueryBuilder(ormMeta.db).from(middleTable)
    }

    /**
     * 查询从表
     *     根据hasMany/hasOne的关联关系，来构建查询条件
     *     通过join中间表 查从表
     *
     * @param item (主表)当前Orm对象
     * @param fkInMany hasMany关系下的单个外键值Any|对象IOrm，如果为null，则更新所有关系, 否则更新单个关系
     * @return
     */
    public override fun queryRelated(item: IOrm, fkInMany: Any?): OrmQueryBuilder? {
        // 通过join中间表 查从表
        val pk: DbKeyValues = item.gets(primaryProp) // 主键
        if(item.isPkEmpty(pk))
            return null;

        val tableAlias = middleTable + '.'
        val query = buildQuery() // 中间表.远端外键 = 从表.远端主键
                .where(foreignKey.wrap(tableAlias) /*tableAlias + foreignKey*/, pk) as OrmQueryBuilder // 中间表.外键 = 主表.主键
        if (fkInMany != null) { // hasMany关系下过滤单个关系
            val farPk = farPrimaryProp.getsFrom(fkInMany)
            query.where(farForeignKey.wrap(tableAlias) /*tableAlias + farForeignKey*/, farPk)
        }
        return query
    }

    /**
     * 查询从表
     *     根据hasMany/hasOne的关联关系，来构建查询条件
     *     通过join中间表 查从表
     *
     * @param items (主表)当前Orm列表
     * @return
     */
    public override fun queryRelated(items: Collection<out IOrm>): OrmQueryBuilder? {
        if(items.isEmpty())
            return null

        // 通过join中间表 查从表
        val tableAlias = middleTable + '.'
        return buildQuery() // 中间表.远端外键 = 从表.远端主键
                .whereIn(foreignKey.wrap(tableAlias)/*middleTable + '.' + foreignKey*/, items.collectColumn(primaryProp)) as OrmQueryBuilder // 中间表.外键 = 主表.主键
    }

    /**
     * 查询关联表
     *    自动根据关联关系，来构建查询条件
     *    通过join中间表 查从表
     *    主要用于级联删除
     *
     * @param subquery (主表)当前子查询
     * @return
     */
    override fun queryRelated(subquery: IDbQueryBuilder): OrmQueryBuilder{
        // 通过join中间表 查从表
        val tableAlias = middleTable + '.'
        val subQueryAlias = "sub_" + model.modelName
        return buildQuery() // 中间表.远端外键 = 从表.远端主键
                .join(DbExpr(subquery.copy(true).select(primaryKey.wrap(subQueryAlias + ".") /* TODO: 加子查询内的表前缀 */), subQueryAlias), "INNER") // select 主表.主键
                .on(foreignKey.wrap(tableAlias) /*middleTable + foreignKey*/, primaryKey.wrap(subQueryAlias + ".") /*subQueryAlias + primaryKey*/) as OrmQueryBuilder // 中间表.外键 = 主表.主键
    }

    /**
     * 对query builder联查关联表(通过中间表联查从表)
     *    中间表.外键 = 主表.主键
     *    中间表.远端外键 = 从表.远端主键
     *
     * @param query
     * @param thisName 当前表别名 = 主表别名
     * @param relatedName 关联表别名 = 从表别名
     * @return
     */
    override fun applyQueryJoinRelated(query: OrmQueryBuilder, thisName:String, relatedName: String) {
        val master = ormMeta
        val masterName = thisName
        val slaveRelation = this
        val slaveName = relatedName

        // 检查并添加联查关系
        if(!query.addJoinOne(slaveName))
            return

        // 准备条件
        val masterPk:DbKeyNames = slaveRelation.primaryKey.map {  // 主表.主键
            masterName + "." + it // masterName + "." + slaveRelation.primaryKey
        };
        val middleFk:DbKeyNames = slaveRelation.foreignKey.map { // 中间表.外键
            slaveRelation.middleTable + '.' + it // slaveRelation.middleTable + '.' + slaveRelation.foreignKey
        }

        val slave = slaveRelation.ormMeta
        val slavePk2:DbKeyNames = slaveRelation.farPrimaryKey.map { // 从表.远端主键
            slaveName + "." + it // slaveName + "." + slaveRelation.farPrimaryKey
        }
        val middleFk2:DbKeyNames = slaveRelation.farForeignKey.map {  // 中间表.远端外键
            slaveRelation.middleTable + '.' + it // slaveRelation.middleTable + '.' + slaveRelation.farForeignKey
        }

        // 查中间表
        query.join(slaveRelation.middleTable).on(masterPk, middleFk) // 中间表.外键 = 主表.主键

        // 查从表
        query.join(DbExpr(slave.table, slaveName), "LEFT").on(slavePk2, middleFk2) // 中间表.远端外键 = 从表.远端主键
    }

    /**
     * 添加关系（插入中间表）
     *
     * @param item (主表)本模型对象
     * @param value (从表)外键值Any | 关联对象IOrm
     * @return
     */
    override fun addRelation(item: IOrm, value: Any): Boolean{
        // 插入中间表
        val query = DbQueryBuilder(ormMeta.db)
                .from(middleTable)
                .insertColumns(*foreignKey.columns, *farForeignKey.columns)
        // 中间表.外键 = 主表.主键
        val pk = primaryProp.getsFrom(item)
        if(value is Collection<*>) { // 多个
            for (farPkItem in value) {
                //中间表.远端外键 = 从表.远端主键
                val farPk = farPrimaryProp.getsFrom(farPkItem)
                query.insertValue(pk, farPk)
            }
        }else{ // 单个
            //中间表.远端外键 = 从表.远端主键
            val farPk = farPrimaryProp.getsFrom(value)
            query.insertValue(pk, farPk)
        }
        return query.insert() > 0
    }

    /**
     * 删除关系（删除中间表记录）
     *
     * @param item (主表)本模型对象
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    override fun removeRelation(item: IOrm, fkInMany: Any?): Boolean{
        val pk: DbKeyValues = item.gets(primaryProp)
        if(item.isPkEmpty(pk))
            return false;

        // 删除中间表记录
        val query = queryMiddleTable().where(foreignKey, pk) // 中间表.外键 = 主表.主键
        if (fkInMany != null) { // hasMany关系下过滤单个关系
            val farPk = farPrimaryProp.getsFrom(fkInMany)
            query.where(farForeignKey, farPk) // 中间表.远端外键 = 从表.远端主键
        }

        return query.delete()
    }

    /**
     * 删除关系（删除中间表记录）
     *
     * @param @param relatedQuery (从表)关联对象的查询
     * @return
     */
    override fun removeRelation(relatedQuery: IDbQueryBuilder): Boolean{
        // 删除中间表记录
        return queryMiddleTable()
                .join(DbExpr(relatedQuery.copy(true).select(farPrimaryKey.wrap("_slave.")), "_slave"), "INNER") // select 从表.远端主键
                .on(farForeignKey.wrap(middleTable + "."), farPrimaryKey.wrap("_slave.")) // // 中间表.远端外键 = 从表.远端主键
                .delete()
    }

    /**
     * 删除当前层关联对象
     *
     * @param relatedQuery 关联对象的查询
     * @return
     */
    protected override fun doDeleteRelated(relatedQuery: IDbQueryBuilder): Boolean {
        return relatedQuery.delete()
    }
}
