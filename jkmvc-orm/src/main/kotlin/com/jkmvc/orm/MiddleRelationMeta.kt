package com.jkmvc.orm

import com.jkmvc.db.DbExpr
import com.jkmvc.db.DbQueryBuilder
import com.jkmvc.db.IDbQueryBuilder
import kotlin.reflect.KClass

/**
 * 有中间表的关联关系的元数据
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
 */
class MiddleRelationMeta(
        sourceMeta:IOrmMeta, /* 源模型元数据 */
        type:RelationType /* 关联关系 */,
        model: KClass<out IOrm> /* 关联模型类型 */,
        foreignKey:String /* 外键 */,
        primaryKey:String/* 主键 */,
        public val middleTable:String/* 中间表 */,
        public val farForeignKey:String /* 远端外键 */,
        public val farPrimaryKey:String/* 远端主键 */,
        conditions:Map<String, Any?> = emptyMap() /* 查询条件 */
) : RelationMeta(sourceMeta, type, model, foreignKey, primaryKey, conditions) {

    /**
     * 远端主键属性
     *   与 farPrimaryKey 对应
     */
    public val farPrimaryProp:String = sourceMeta.column2Prop(farPrimaryKey)

    /**
     *  远端外键属性
     *    与 farForeignKey 对应
     */
    public val farForeignProp:String = sourceMeta.column2Prop(farForeignKey)

    /**
     * 中间表的外键字段别名
     *    用在 OrmQueryBuilder.findAll() 联查从表时，绑定主对象
     *    不能使用foreignKey, 因为中间表的该字段可能与从表字段重名
     */
    public val middleForeignKey = foreignKey + '_'

    /**
     * 中间表的外键属性
     *    与 middleForeignKey 对应
     */
    public val middleForeignProp = sourceMeta.column2Prop(middleForeignKey)

    /**
     * 构建查询：通过join中间表来查询从表
     * @return
     */
    protected fun buildQuery(): OrmQueryBuilder {
        return queryBuilder()
                .select(model.modelName + ".*", DbExpr(middleTable + '.' + foreignKey, middleForeignKey)) // 查关联字段：中间表.外键 = 主表.主键，用在 OrmQueryBuilder.findAll() 联查从表时，绑定主对象
                .join(middleTable).on(middleTable + '.' + farForeignKey, "=", model.modelName + '.' + farPrimaryKey) as OrmQueryBuilder // 中间表.远端外键 = 从表.远端主键
    }

    /**
     * 查询中间表
     *
     * @param item
     * @param fkInMany hasMany关系下的单个关联对象，如果为null，则更新所有关系, 否则更新单个关系
     * @return
     */
    public fun queryMiddleTable(item: IOrm, fkInMany: IOrm): IDbQueryBuilder? {
        return queryMiddleTable(item, fkInMany as Any)
    }

    /**
     * 查询中间表
     *
     * @param item
     * @param fkInMany hasMany关系下的单个外键值Any|对象IOrm，如果为null，则更新所有关系, 否则更新单个关系
     * @return
     */
    public fun queryMiddleTable(item: IOrm, fkInMany: Any? = null): IDbQueryBuilder? {
        val pk: Any? = item[primaryProp]
        if(pk == null)
            return null;
        val query = DbQueryBuilder(ormMeta.db, middleTable).where(foreignKey, "=", pk)
        if (fkInMany != null) { // hasMany关系下过滤单个关系
            val farPk = if(fkInMany is IOrm) fkInMany[farPrimaryProp] else fkInMany
            query.where(farForeignKey, fkInMany)
        }
        return query;
    }

    /**
     * 插入中间表
     *
     * @param pk IOrm 主对象
     * @param farPk IOrm 从对象
     * @return
     */
    public fun insertMiddleTable(pk:IOrm, farPk:IOrm): Int {
        return insertMiddleTable(pk as Any, farPk as Any)
    }

    /**
     * 插入中间表
     *
     * @param pk Any主表主键 | IOrm 主对象
     * @param farPk Any从表主键 | IOrm 从对象
     * @return
     */
    public fun insertMiddleTable(pk:Any, farPk:Any): Int {
        val pk2 = if(pk is IOrm) pk[primaryProp] else pk
        val farPk2 = if(farPk is IOrm) farPk[farPrimaryProp] else farPk
        return DbQueryBuilder(ormMeta.db, middleTable).insertColumns(foreignKey, farForeignKey).value(pk2, farPk2).insert()
    }

    /**
     * 查询从表
     *     根据hasMany/hasOne的关联关系，来构建查询条件
     *
     * @param item Orm对象
     * @param fkInMany hasMany关系下的单个外键值Any|对象IOrm，如果为null，则更新所有关系, 否则更新单个关系
     * @param withTableAlias 是否带表前缀
     * @return
     */
    public override fun queryRelated(item: IOrm, fkInMany: Any?, withTableAlias:Boolean): OrmQueryBuilder? {
        // 通过join中间表 查从表
        val pk: Any? = item[primaryProp] // 主键
        if(pk == null)
            return null;
        val tableAlias = middleTable + '.'
        val query = buildQuery() // 中间表.远端外键 = 从表.远端主键
                .where(tableAlias + foreignKey, "=", pk) as OrmQueryBuilder // 中间表.外键 = 主表.主键
        if (fkInMany != null) { // hasMany关系下过滤单个关系
            val farPk = if(fkInMany is IOrm) fkInMany[farPrimaryProp] else fkInMany
            query.where(tableAlias + farForeignKey, farPk)
        }
        return query
    }

    /**
     * 查询从表
     *     根据hasMany/hasOne的关联关系，来构建查询条件
     *
     * @param items Orm列表
     * @return
     */
    public override fun queryRelated(items: Collection<out IOrm>): OrmQueryBuilder {
        // 通过join中间表 查从表
        return buildQuery() // 中间表.远端外键 = 从表.远端主键
                .where(middleTable + '.' + foreignKey,  "IN", items.collectColumn(primaryProp)) as OrmQueryBuilder // 中间表.外键 = 主表.主键
    }
}
