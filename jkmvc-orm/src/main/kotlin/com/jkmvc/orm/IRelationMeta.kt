package com.jkmvc.orm

import com.jkmvc.db.recordTranformer
import kotlin.reflect.KClass

/**
 * 关联类型
 * @author shijianhang
 * @date 2016-10-10
 */
enum class RelationType {
    /**
     * 关联关系 - 有一个
     *    当前表是主表, 关联表是从表
     */
    BELONGS_TO,

    /**
     * 关联关系 - 从属于
     *    当前表是从表, 关联表是主表
     */
    HAS_ONE,

    /**
     * 关联关系 - 有多个
     * 	当前表是主表, 关联表是从表
     */
    HAS_MANY;
}

/**
 * 关联关系的元数据
 * @author shijianhang
 * @date 2016-10-10
 */
interface IRelationMeta {

    /**
     * 源模型元数据
     */
    val sourceMeta:IOrmMeta;

    /**
     *  关联关系
     */
    val type:RelationType;

    /**
     * 关联模型类型
     */
    val model: KClass<out IOrm>;

    /**
     *  主键
     *    一般情况下，是源模型中的主键（sourceMeta.primaryKey），不需要指定
     *    但是某些情况下，是源模型的业务主键，需要手动指定
     */
    val primaryKey:String;

    /**
     *  外键
     */
    val foreignKey:String;

    /**
     *  查询条件
     */
    val conditions:Map<String, Any?>

    /**
     * 主键属性
     */
    val primaryProp:String

    /**
     *  外键属性
     */
    val foreignProp:String;

    /**
     * 获得关联模型的元数据
     *  伴随对象就是元数据
     */
    val ormMeta: IOrmMeta
        get() = model.modelOrmMeta

    /**
     * 记录转换器
     */
    val recordTranformer: (MutableMap<String, Any?>) -> IOrm
        get()= model.recordTranformer

    /**
     * 获得关联模型的查询器
     */
    fun queryBuilder():OrmQueryBuilder {
        // 关联查询 + 条件
        return ormMeta.queryBuilder().wheres(conditions) as OrmQueryBuilder
    }

    /**
     * 创建模型实例
     * @return
     */
    fun newModelInstance(): IOrm {
        return model.java.newInstance();
    }

}
