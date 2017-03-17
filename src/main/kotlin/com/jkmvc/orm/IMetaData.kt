package com.jkmvc.orm

import com.jkmvc.db.Db
import com.jkmvc.db.IDbQueryBuilder
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmName

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
        protected val props:ConcurrentHashMap<String, ReadWriteProperty<IOrm, *>> by lazy{
            ConcurrentHashMap<String, ReadWriteProperty<IOrm, *>>()
        }

        /**
         * 生成并缓存属性代理
         *   代理模型的属性读写
         */
        protected fun <T> getProperty(key:String): ReadWriteProperty<IOrm, T>{
            return props.getOrPut(key){
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
                }
            } as ReadWriteProperty<IOrm, T>;
        }
    }

    /**
     * 模型类
     */
    public abstract val model: KClass<out IOrm>

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
    public abstract val relations:ConcurrentHashMap<String, MetaRelation>

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
     * 生成属性代理
     */
    public inline fun <reified T> property(): ReadWriteProperty<IOrm, T>{
        val key:String = T::class.jvmName // 属性类型作为key: 同一类型的属性，共用一个代理
        return getProperty<T>(key);
    }

    /**
     * 生成属性代理 + 设置关联关系(belongs to)
     */
    public inline fun <reified T:IOrm> belongsTo(name:String, foreignKey:String, noinline conditions:((IDbQueryBuilder) -> Unit)? = null): ReadWriteProperty<IOrm, T>{
        // 设置关联关系
        relations.getOrPut(name){
            MetaRelation(RelationType.BELONGS_TO, T::class, foreignKey, conditions)
        }

        // 生成属性代理对象
        val key:String = model.jvmName + "-" + name; // 属性全名作为key: 同一个模型的同一个属性，共用一个代理
        return getProperty<T>(key);
    }

    /**
     * 设置关联关系(has one)
     */
    public inline fun <reified T:IOrm> hasOne(name:String, foreignKey:String, noinline conditions:((IDbQueryBuilder) -> Unit)? = null): ReadWriteProperty<IOrm, T>{
        // 设置关联关系
        relations.getOrPut(name){
            MetaRelation(RelationType.HAS_ONE, T::class, foreignKey, conditions)
        }

        // 生成属性代理对象
        val key:String = model.jvmName + "-" + name; // 属性全名作为key: 同一个模型的同一个属性，共用一个代理
        return getProperty<T>(key);
    }

    /**
     * 设置关联关系(has many)
     */
    public inline fun <reified T:IOrm> hasMany(name:String, foreignKey:String, noinline conditions:((IDbQueryBuilder) -> Unit)? = null): ReadWriteProperty<IOrm, List<T>>{
        // 设置关联关系
        relations.getOrPut(name){
            MetaRelation(RelationType.HAS_MANY, T::class, foreignKey, conditions)
        }

        // 生成属性代理对象
        val key:String = model.jvmName + "-" + name; // 属性全名作为key: 同一个模型的同一个属性，共用一个代理
        return getProperty<List<T>>(key);
    }

}
