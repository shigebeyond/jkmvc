package com.jkmvc.db

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.primaryConstructor

/**
 * sql构建器
 *   依次继承 DbQueryBuilderAction 处理动作子句 + DbQueryBuilderDecoration 处理修饰子句
 *   提供select/where等类sql的方法, 但是调用方法时, 不直接拼接sql, 而是在compile()时才延迟拼接sql, 因为调用方法时元素可以无序, 但生成sql时元素必须有序
 *
 * 注：为什么不是接口，而是抽象类？
 *    因为我需要实现 inline fun <reified T:Any> find(): T? / inline fun <reified T:Any>  findAll(): List<T>
 *    这两个方法都需要具体化泛型，因此需要内联实现inline，但是inline不能用于接口方法/抽象方法，因此我直接在该类中实现这两个方法，该类也只能由接口变为抽象类
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-13
 *
 */
abstract class IDbQueryBuilder:IDbQueryBuilderAction, IDbQueryBuilderDecoration
{
    companion object{
        /**
         * 缓存记录构造器
         */
        protected val recordConstructors:MutableMap<KClass<*>, KFunction<*>?> by lazy {
            ConcurrentHashMap<KClass<*>, KFunction<*>?>();
        }

        /**
         * 获得记录构造器
         */
        protected fun getRecordConstructor(cls: KClass<*>): KFunction<*>? {
            return recordConstructors.getOrPut(cls){
                cls.findConstructor(listOf(MutableMap::class.java))
            }
        }

        /**
         * 获得记录转换器
         */
        protected inline fun <reified T:Any> getRecordTranformer(): ((MutableMap<String, Any?>) -> T) {
            val cls = T::class;
            // 1 如果是map类，则直接返回
            if(Map::class.java.isAssignableFrom(cls.java)){
                return {
                    it as T;
                }
            }
            // 2 否则，调用其构造函数
            // 获得类的构造函数
            val construtor = getRecordConstructor(cls);
            if(construtor == null)
                throw RuntimeException("类${cls}没有构造函数constructor(MutableMap)");

            // 调用构造函数
            return {
                construtor.call(it) as T; // 转换一行数据: 直接调用构造函数
            }
        }
    }

    /**
     * 编译sql
     *
     * @param string action sql动作：select/insert/update/delete
     * @return Pair(sql, 参数)
     */
    public abstract fun compile(action:String): Pair<String, List<Any?>>;

    /**
     * 查找多个： select 语句
     *
     * @return array
     */
    public inline fun <reified T:Any>  findAll(): List<T> {
        // 1 编译
        val (sql, params) = compile("select");

        // 2 执行 select
        return db.queryRows<T>(sql, params, getRecordTranformer<T>())
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @return object
     */
    public inline fun <reified T:Any> find(): T? {
        // 1 编译
        val (sql, params) = compile("select");

        // 2 执行 select
        return db.queryRow<T>(sql, params, getRecordTranformer<T>());
    }

    /**
     * 统计行数： count语句
     * @return int
     */
    public abstract fun count():Long;

    /**
     * 插入：insert语句
     * @return int 新增的id
     */
    public abstract fun insert():Int;

    /**
     *	更新：update语句
     *	@return	bool
     */
    public abstract fun update():Boolean;

    /**
     *	删除
     *	@return	bool
     */
    public abstract fun delete():Boolean;
}