package net.jkcode.jkmvc.orm.jphp

import net.jkcode.jkmvc.orm.*
import net.jkcode.jphp.ext.call_getter
import net.jkcode.jphp.ext.call_setter
import net.jkcode.jphp.ext.toJavaObject
import net.jkcode.jphp.ext.toPureArray
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.ext.java.JavaObject
import php.runtime.lang.BaseWrapper
import php.runtime.memory.ArrayMemory
import php.runtime.memory.ObjectMemory
import php.runtime.memory.support.MemoryUtils
import php.runtime.reflection.ClassEntity
import kotlin.reflect.KClass

/**
 * 包装model
 *    java中的实例化： val model = PModel.of(env, orm实例)
 *    php中的实例化: $model = new Model("net.jkcode.jkmvc.tests.model.UserModel");
 *    php中的方法调用: $model["id"] = 1; $model->create();
 */
@Reflection.Name("Model")
@Reflection.Namespace(JkmvcOrmExtension.NS)
open class PModel(env: Environment, clazz: ClassEntity) : BaseWrapper<JavaObject>(env, clazz) {

    /**
     * orm元数据
     */
    protected lateinit var ormMeta: OrmMeta

    /**
     * model对象
     *   递延加载，可能不会创建
     */
    protected var model: IOrm? = null

    /**
     * 获得或创建model对象
     */
    protected fun getOrCreateModel(): IOrm{
        if(model == null)
            model = ormMeta.newInstance()
        return model!!
    }

    /**
     * 构造函数
     */
    @Reflection.Signature
    fun __construct(className: String): Memory {
        val clazz = Class.forName(className).kotlin as KClass<out Orm>
        ormMeta = clazz.modelOrmMeta
        return Memory.NULL
    }

    /**
     * 获得查询构建器
     */
    @Reflection.Signature
    fun queryBuilder(env: Environment): ObjectMemory {
        val qb = PQueryBuilder.of(env, ormMeta.queryBuilder())
        return qb.objMem
    }

    /**
     * 读属性
     */
    @Reflection.Signature(Reflection.Arg("name"))
    fun __get(env: Environment, vararg args: Memory): Memory {
        val name = args[0].toString()

        // 1 先尝试调用getter
        var v = call_getter(env, getOrCreateModel(), name)
        if (v != null)
            return v

        // 2 再读字段
        return MemoryUtils.valueOf(env, getOrCreateModel().get(name))
        return Memory.NULL
    }

    /**
     * 写属性
     */
    @Reflection.Signature(value = [Reflection.Arg("name"), Reflection.Arg("value")])
    fun __set(env: Environment, vararg args: Memory): Memory {
        val name = args[0].toString()
        // 1 先尝试调用setter
        val v = call_setter(env, getOrCreateModel(), name, args[1])
        if (v != null)
            return v

        // 2 再写字段
        getOrCreateModel().set(name, args[1].toJavaObject())
        return Memory.NULL
    }

    /**
     * 根据主键值来加载数据
     */
    @Reflection.Signature
    fun load(env: Environment, pk: Memory) {
        val pks: Array<Any> = buildPK(pk)
        getOrCreateModel().loadByPk(*pks)
    }

    /**
     * 根据主键值来查找数据
     */
    @Reflection.Signature
    fun find(env: Environment, pk: Memory): Map<String, Any?>? {
        val pks: Array<Any> = buildPK(pk)
        //return ormMeta.findByPk<?>(DbKeyValues(*pks))
        return ormMeta.queryBuilder().where(ormMeta.primaryKey, DbKeyValues(*pks)).findMap()
    }

    /**
     * 构建主键参数
     */
    private fun buildPK(pk: Memory): Array<Any> {
        val pks: Array<Any>
        if (pk.isArray)
            pks = (pk as ArrayMemory).toPureArray() as Array<Any>
        else
            pks = arrayOf(pk.toJavaObject()!!)
        return pks
    }

    /**
     * 保存数据
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun save(withHasRelations: Boolean = false): Boolean{
        return getOrCreateModel().save(withHasRelations)
    }

    /**
     * 插入数据: insert sql
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @param checkPkExists 是否检查主键存在
     * @return 新增数据的主键
     */
    @Reflection.Signature
    @JvmOverloads
    fun create(withHasRelations: Boolean = false): Long{
        return getOrCreateModel().create(withHasRelations)
    }

    /**
     * 更新数据: update sql
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun update(withHasRelations: Boolean = false): Boolean{
        return getOrCreateModel().update(withHasRelations)
    }

    /**
     * 删除数据: delete sql
     * @param withHasRelations 是否连带删除 hasOne/hasMany 的关联关系
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun delete(withHasRelations: Boolean = false): Boolean{
        return getOrCreateModel().delete(withHasRelations)
    }

    @Reflection.Signature
    open fun __toString(env: Environment): String {
        return getOrCreateModel().toString()
    }

    companion object {

        /**
         * java中获得单例
         */
        @JvmStatic
        fun of(env: Environment, model: Orm): PModel {
            val obj = PModel(env, env.fetchClass(JkmvcOrmExtension.NS + "\\Model"))
            obj.model = model
            obj.ormMeta = model.ormMeta
            return obj
        }
    }
}