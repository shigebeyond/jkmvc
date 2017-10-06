package com.jkmvc.orm

import kotlin.reflect.KClass

/**
 * 关联关系的元数据
 */
data class RelationMeta(public override val sourceMeta:IOrmMeta, /* 源模型元数据 */
                        public override val type:RelationType /* 关联关系 */,
                        public override val model: KClass<out IOrm> /* 关联模型类型 */,
                        public override val foreignKey:String /* 外键 */,
                        public override val primaryKey:String/* 主键 */,
                        public override val conditions:Map<String, Any?> = emptyMap() /* 查询条件 */
    ): IRelationMeta {

    /**
     * 主键属性
     */
    public override val primaryProp:String = sourceMeta.column2Prop(primaryKey)

    /**
     *  外键属性
     */
    public override val foreignProp:String = sourceMeta.column2Prop(foreignKey)

    /**
     * 构造函数
     * @param sourceMeta 源模型元数据
     * @param type 关联关系
     * @param model 关联模型类型
     * @param foreignKey 外键
     * @param conditions 查询条件
     */
    /*public constructor(sourceMeta:IOrmMeta, type:RelationType, model: KClass<out IOrm>, foreignKey:String, conditions:Map<String, Any?> = emptyMap()):this(sourceMeta, type, model, foreignKey, sourceMeta.primaryKey, conditions){
    }*/
}