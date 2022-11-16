package net.jkcode.jkmvc.orm.jphp

import net.jkcode.jkmvc.db.Db
import net.jkcode.jkmvc.query.DbQueryBuilder
import net.jkcode.jphp.ext.toPureList
import php.runtime.Memory
import php.runtime.annotation.Reflection
import php.runtime.env.Environment
import php.runtime.ext.java.JavaObject
import php.runtime.lang.BaseWrapper
import php.runtime.memory.ArrayMemory
import php.runtime.memory.ObjectMemory
import php.runtime.reflection.ClassEntity

/**
 * 包装db
 *    php中的实例化: $db = PDb::instance("default");
 *    php中的方法调用: $db->get("key");
 */
@Reflection.Name("Db")
@Reflection.Namespace(JkmvcOrmExtension.NS)
open class PDb(env: Environment, clazz: ClassEntity) : BaseWrapper<JavaObject>(env, clazz) {

    // db配置名
    lateinit var name: String

    /**
     * 代理的db
     *   必须实时获得，因为代理的db的作用域是请求级的
     */
    val proxyDb: Db
        get() = Db.instance(name)

    @Reflection.Signature
    fun __construct(name: String): Memory {
        if (name.isBlank())
            this.name = "default"
        else
            this.name = name
        return Memory.NULL
    }

    /**
     * 获得查询构建器
     */
    @Reflection.Signature
    fun queryBuilder(env: Environment): ObjectMemory {
        val qb = PQueryBuilder.of(env, DbQueryBuilder(proxyDb))
        return qb.objMem
    }

    /**
     * 开启事务
     */
    @Reflection.Signature
    fun begin(){
        proxyDb.begin()
    }

    /**
     * 提交
     */
    @Reflection.Signature
    fun commit():Boolean{
        return proxyDb.commit()
    }

    /**
     * 回滚
     */
    @Reflection.Signature
    fun rollback():Boolean{
        return proxyDb.rollback()
    }

    /**
     * 预览sql
     * @param sql
     * @param params sql参数
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun previewSql(sql: String, params: ArrayMemory? = null): String{
        return proxyDb.previewSql(sql, params.toPureList())
    }

    /**
     * 查询多行
     * @param sql
     * @param params 参数
     * @param result
     * @param transform 转换行的函数
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun query(sql: String, params: ArrayMemory? = null): List<Map<String, Any?>> {
        return proxyDb.queryMaps(sql, params.toPureList())
    }

    /**
     * 执行更新
     * @param sql
     * @param params
     * @param generatedColumn 返回的自动生成的主键名
     * @return
     */
    @Reflection.Signature
    @JvmOverloads
    fun execute(sql: String, params: ArrayMemory? = null): Long{
        val generatedColumn:String? = null
        return proxyDb.execute(sql, params.toPureList(), generatedColumn)
    }

    /**
     * 批量更新: 每次更新sql参数不一样
     *
     * @param sql
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     */
    @Reflection.Signature
    fun batchExecute(sql: String, paramses: ArrayMemory, paramSize:Int){
        proxyDb.batchExecute(sql, paramses.toPureList(), paramSize)
    }

    companion object {

        /**
         * 获得单例
         */
        @Reflection.Signature
        @JvmStatic
        fun instance(env: Environment, name: String = "default"): Memory {
            val db = PDb(env, env.fetchClass(JkmvcOrmExtension.NS + "\\Db"))
            db.name = name
            return ObjectMemory(db)
        }
    }

}