package net.jkcode.jkmvc.orm.jphp

import net.jkcode.jkmvc.orm.*
import net.jkcode.jkutil.common.newInstance
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
 *    php中的实例化: $model = new Model("net.jkcode.jkmvc.tests.model.UserModel");
 *    php中的方法调用: $model["id"] = 1; $model->create();
 */
@Reflection.Name("Model")
@Reflection.Namespace(JkmvcOrmExtension.NS)
open class PModel(env: Environment, clazz: ClassEntity) : BaseWrapper<JavaObject>(env, clazz) {

    // model名
    lateinit var name: String

    // model类
    lateinit var modelClass: KClass<out Orm>

    /**
     * model对象
     *   递延加载，可能不会创建
     */
    val model: Orm by lazy{
        modelClass.newInstance() as Orm
    }

    /**
     * orm元数据
     */
    val ormMeta: OrmMeta
        get() = modelClass.modelOrmMeta

    /**
     * 构造函数
     */
    @Reflection.Signature
    fun __construct(name: String): Memory {
        this.name = name
        modelClass = Class.forName(name).kotlin as KClass<out Orm>
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
        var v = call_getter(env, model, name)
        if (v != null)
            return v

        // 2 再读字段
        return MemoryUtils.valueOf(env, model.get(name))
        return Memory.NULL
    }

    /**
     * 写属性
     */
    @Reflection.Signature(value = [Reflection.Arg("name"), Reflection.Arg("value")])
    fun __set(env: Environment, vararg args: Memory): Memory {
        val name = args[0].toString()
        // 1 先尝试调用setter
        val v = call_setter(env, model, name, args[1])
        if (v != null)
            return v

        // 2 再写字段
        model.set(name, args[1].toJavaObject())
        return Memory.NULL
    }

    /**
     * 根据主键值来加载数据
     */
    @Reflection.Signature
    fun load(env: Environment, pk: Memory) {
        val pks: Array<Any> = buildPK(pk)
        model.loadByPk(*pks)
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
    fun save(): Boolean{
        val withHasRelations = false
        return model.save(withHasRelations)
    }

    /**
     * 插入数据: insert sql
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @param checkPkExists 是否检查主键存在
     * @return 新增数据的主键
     */
    @Reflection.Signature
    fun create(): Long{
        val withHasRelations = false
        return model.create(withHasRelations)
    }

    /**
     * 更新数据: update sql
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @return
     */
    @Reflection.Signature
    fun update(): Boolean{
        val withHasRelations = false
        return model.update(withHasRelations)
    }

    /**
     * 删除数据: delete sql
     * @param withHasRelations 是否连带删除 hasOne/hasMany 的关联关系
     * @return
     */
    @Reflection.Signature
    fun delete(): Boolean{
        val withHasRelations = false
        return model.delete(withHasRelations)
    }

    @Reflection.Signature
    open fun __toString(env: Environment): String {
        return model.toString()
    }
}