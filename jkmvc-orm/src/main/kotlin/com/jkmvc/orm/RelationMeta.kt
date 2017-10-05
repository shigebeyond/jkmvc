package com.jkmvc.orm

import kotlin.reflect.KClass

/**
 * 关联关系的元数据
 */
data class RelationMeta(public override val type:RelationType /* 关联关系 */, public override val model: KClass<out IOrm> /* 关联模型类型 */, public override var foreignKey:String /* 外键 */, public override val conditions:Map<String, Any?> = emptyMap() /* 查询条件 */): IRelationMeta {
}