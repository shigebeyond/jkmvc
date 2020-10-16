package net.jkcode.jkmvc.orm.relation

import net.jkcode.jkmvc.db.DbResultRow
import net.jkcode.jkmvc.orm.*
import net.jkcode.jkmvc.query.IDbQueryBuilder
import kotlin.reflect.KClass

/**
 * 关联关系
 *
 * 关于 cascadeDeleted:
 *      只对 hasOne/hasMany 有效, 对 belongsTo/hasOneThrough/hasManyThrough 无效
 *      对 belongsTo, 你敢删除 belongsTo 关系的主对象？
 *      对 hasOneThrough/hasManyThrough, 都通过中间表来关联了, 两者之间肯定是独立维护的, 只删除关联关系就好, 不删除关联对象
 *
 * @author shijianhang
 * @date 2016-10-10
 */
interface IRelation {

    /**
     * 是否一对一, 否则一对多
     */
    val one2one: Boolean

    /**
     * 是否是`有一个`关系
     *    当前表是主表, 关联表是从表
     */
    val isBelongsTo: Boolean

    /**
     * 是否是`从属于`关系
     *    当前表是从表, 关联表是主表
     */
    val isHasOne: Boolean

    /**
     * 是否是`有多个`关系
     * 	当前表是主表, 关联表是从表
     */
    val isHasMany: Boolean

    /**
     * 关系名
     */
    val name: String

    /**
     * 源模型元数据
     */
    val srcOrmMeta: IOrmMeta;

    /**
     * 关联模型类型
     */
    val model: KClass<out IOrm>;

    /**
     *  主键
     *    一般情况下，是源模型中的主键（srcOrmMeta.primaryKey），不需要指定
     *    但是某些情况下，是源模型的业务主键，需要手动指定
     */
    val primaryKey: DbKeyNames;

    /**
     *  外键
     */
    val foreignKey: DbKeyNames;

    /**
     *  查询条件
     */
    val conditions:Map<String, Any?>

    /**
     * 是否级联删除
     */
    val cascadeDeleted: Boolean

    /**
     * 检查主键为空的规则
     */
    val pkEmptyRule: PkEmptyRule

    /**
     * 主键属性
     */
    val primaryProp: DbKeyNames

    /**
     *  外键属性
     */
    val foreignProp: DbKeyNames

    /**
     * 外键对应字段的默认值
     */
    val foreignKeyDefault: DbKeyValues

    /**
     * 本模型作为主表
     */
    val thisAsMaster: Boolean

    /**
     * 主表模型元数据
     */
    val masterOrmMeta: IOrmMeta
        get() = if (thisAsMaster) srcOrmMeta else ormMeta

    /**
     * 从表模型元数据
     */
    val slaveOrmMeta: IOrmMeta
        get() = if (thisAsMaster) ormMeta else srcOrmMeta

    /**
     * 本模型键属性
     */
    val thisProp: DbKeyNames
        get() = if(thisAsMaster)
                    primaryProp // 主表.主键
                else
                    foreignProp // 从表.外键

    /**
     *  关联模型键属性
     *    HasNThroughRelation 会改写
     */
    val relatedProp: DbKeyNames
        get() = if(thisAsMaster)
                    foreignProp // 从表.外键
                else
                    primaryProp // 主表.主键

    /**
     * 空值, 用作关联属性值
     */
    val emptyValue: Any?
        get() = if(one2one) null else emptyList<Any>()

    /**
     * 获得关联模型的元数据
     *  伴随对象就是元数据
     */
    val ormMeta: IOrmMeta
        get() = model.modelOrmMeta

    /**
     * 行转换器
     */
    val modelRowTransformer: (DbResultRow) -> IOrm
        get()= model.modelRowTransformer

    /**
     * 获得关联模型的查询器
     */
    fun queryBuilder(): OrmQueryBuilder {
        // 关联查询 + 条件
        return ormMeta.queryBuilder().wheres(conditions) as OrmQueryBuilder
    }

    /**
     * 创建模型实例
     * @return
     */
    fun newModelInstance(): IOrm {
        //return model.java.newInstance();
        return ormMeta.newInstance()
    }

    /**
     * 查询关联表
     *    自动根据关联关系，来构建查询条件
     *
     * @param orm Orm对象或列表
     * @return
     */
    fun queryRelated(orm: Any): OrmQueryBuilder?{
        return when(orm){
            is IOrm -> queryRelated(orm)
            is Collection<*> -> queryRelated(orm as Collection<out IOrm>)
            is IDbQueryBuilder -> queryRelated(orm)
            else -> throw IllegalArgumentException("Method `relation.queryRelated(parameter)`，noly accept orm object/list as parameter")
        }
    }

    /**
     * 查询关联表
     *    自动根据关联关系，来构建查询条件
     *
     * @param item 当前Orm对象
     * @param fkInMany hasMany关系下的单个外键值，如果为null，则更新所有关系, 否则更新单个关系
     * @return
     */
    fun queryRelated(item: IOrm, fkInMany: Any? = null): OrmQueryBuilder?

    /**
     * 查询关联表
     *    自动根据关联关系，来构建查询条件
     *
     * @param items 当前Orm列表
     * @return
     */
    fun queryRelated(items: Collection<out IOrm>): OrmQueryBuilder?

    /**
     * 查询关联表
     *    自动根据关联关系，来构建查询条件
     *
     * @param subquery 当前子查询
     * @return
     */
    fun queryRelated(subquery: IDbQueryBuilder): OrmQueryBuilder?

    /**
     * 对query builder联查关联表
     *
     * @param query
     * @param thisName 当前表别名
     * @param relatedName 关联表别名
     * @return
     */
    fun applyQueryJoinRelated(query: OrmQueryBuilder, thisName:String, relatedName: String)

    /**
     * 批量设置关系的属性值
     *
     * @param items 本模型对象
     * @param relatedItems 关联模型对象
     */
    fun batchSetRelationProp(items: List<IOrm>, relatedItems: List<IOrm>)

    /**
     * 添加关系（添加从表的外键值）
     *
     * @param item 本模型对象
     * @param value 外键值Any | 关联对象IOrm
     * @return
     */
    fun addRelation(item: IOrm, value: Any): Boolean

    /**
     * 删除关系（删除从表的外键值）
     *
     * @param item 本模型对象
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    fun removeRelation(item: IOrm, fkInMany: Any? = null): Boolean

    /**
     * 删除关系（删除从表的外键值）
     *
     * @param @param relatedQuery 关联对象的查询
     * @return
     */
    fun removeRelation(relatedQuery: IDbQueryBuilder): Boolean

    /**
     * 删除关联对象
     *    一般用于删除 hasOne/hasMany 关系的从对象
     *    你敢删除 belongsTo 关系的主对象？
     *
     * @param item 本模型对象
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    fun deleteRelated(item: IOrm, fkInMany: Any? = null): Boolean

    /**
     * 删除关联对象
     *    一般用于删除 hasOne/hasMany 关系的从对象
     *    你敢删除 belongsTo 关系的主对象？
     *
     * @param relatedQuery 关联对象的查询
     * @param deletedModels 记录删除过的模型
     * @return
     */
    fun deleteRelated(relatedQuery: IDbQueryBuilder, deletedModels: MutableSet<IOrmMeta>): Boolean
}
