package com.jkmvc.orm

import com.jkmvc.db.Db
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * orm的元数据
 * 　两重职责
 *  1 模型映射表的映射元数据，如模型类/数据库/表名
 *  2 代理模型的属性读写
 */
abstract class IMetaData{

    companion object{
        /**
         * 缓存属性代理
         */
        val props:MutableMap<KClass<*>, ReadWriteProperty<IOrm, *>> by lazy{
            ConcurrentHashMap<KClass<*>, ReadWriteProperty<IOrm, *>>()
        }
    }

    /**
     * 模型类
     */
    public abstract val model: KClass<*>

    /**
     * 数据库名
     */
    public abstract val dbName:String

    /**
     * 表名
     */
    public abstract var table:String

    /**
     * 主键
     */
    public abstract val primaryKey:String

    /**
     * 关联关系
     */
    public abstract val relations:Map<String, MetaRelation>?

    /**
     * 数据库
     */
    public abstract val db:Db

    /**
     * 表字段
     */
    public abstract val columns:List<String>

    /**
     * 是否有某个关联关系
     */
    public abstract fun hasRelation(name:String):Boolean;

    /**
     * 获得某个关联关系
     */
    public abstract fun getRelation(name:String):MetaRelation?;

    /**
     * 获得orm查询构建器
     */
    public abstract fun queryBuilder(): OrmQueryBuilder;

    /**
     * 获得属性代理
     *   代理模型的属性读写
     */
    public inline fun <reified T> property(): ReadWriteProperty<IOrm, T>{
        val clazz:KClass<*> = T::class;
        return props.getOrPut(clazz){
            // 生成属性代理对象
            object : ReadWriteProperty<IOrm, T> {
                // 获得属性
                public override operator fun getValue(thisRef: IOrm, property: KProperty<*>): T {
                    return thisRef[property.name]
                }

                // 设置属性
                public override operator fun setValue(thisRef: IOrm, property: KProperty<*>, value: T) {
                    thisRef[property.name] = value
                }
            } as ReadWriteProperty<IOrm, *>
        } as ReadWriteProperty<IOrm, T>;
    }

}
