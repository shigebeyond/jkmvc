package com.jkmvc.orm

import com.jkmvc.common.findStaticFunction
import kotlin.reflect.KClass
import kotlin.reflect.companionObjectInstance

/**
 * 关联类型
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
 */
data class MetaRelation(public val type:RelationType /* 关联关系 */, public val model: KClass<*> /* 关联模型 */, public val foreignKey:String /* 外键 */){

    /**
     * 获得关联模型的元数据
     *  伴随对象就是元数据
     */
    public val metadata:IMetaData
        get() = model.companionObjectInstance as IMetaData
}