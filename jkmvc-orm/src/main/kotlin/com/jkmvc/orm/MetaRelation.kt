package com.jkmvc.orm

import com.jkmvc.db.IDbQueryBuilder
import kotlin.reflect.KClass

/**
 * 关联关系的元数据
 */
data class MetaRelation(public override val type:RelationType /* 关联关系 */, public override val model: KClass<out IOrm> /* 关联模型类型 */, public override var foreignKey:String /* 外键 */, public override val conditions:((IDbQueryBuilder) -> Unit)? = null /* 查询条件 */): IMetaRelation{

    /**
     * 获得关联模型的元数据
     *  伴随对象就是元数据
     */
    public override val metadata:IMetaData
        get() = model.modelMetaData

    /**
     * 获得关联模型的查询器
     */
    public override fun queryBuilder():OrmQueryBuilder {
        // 关联表的查询器
        val qb = metadata.queryBuilder();
        // 添加查询条件
        conditions?.invoke(qb);
        return qb;
    }
}