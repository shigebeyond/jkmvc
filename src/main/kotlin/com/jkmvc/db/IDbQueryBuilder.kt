package com.jkmvc.db

import com.jkmvc.common.findConstructor
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

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
        public fun getRecordConstructor(clazz: KClass<*>): KFunction<*>? {
            return recordConstructors.getOrPut(clazz){
                clazz.findConstructor(listOf(MutableMap::class.java))
            }
        }

        /**
         * 获得记录转换器
         */
        public fun <T:Any> getRecordTranformer(clazz: KClass<T>): ((MutableMap<String, Any?>) -> T) {
            // 1 如果是map类，则直接返回
            if(Map::class.java.isAssignableFrom(clazz.java)){
                return {
                    it as T;
                }
            }
            // 2 否则，调用其构造函数
            // 获得类的构造函数
            val construtor = getRecordConstructor(clazz);
            if(construtor == null)
                throw RuntimeException("类${clazz}没有构造函数constructor(MutableMap)");

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
     * @param KClass<T> clazz 返回对象的类型
     * @return array
     */
    public abstract fun <T:Any>  findAll(clazz:KClass<T>): List<T>;

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param KClass<T> clazz 返回对象的类型
     * @return object
     */
    public abstract fun <T:Any>  find(clazz:KClass<T>): T?;

    /**
     * 查找多个： select 语句
     *  对 findAll(clazz:KClass<T>) 的精简版，直接根据泛型 T 来确定返回对象的类型
     *
     * @return array
     */
    public inline fun <reified T:Any>  findAll(): List<T> {
        // 1 编译
        val (sql, params) = compile("select");

        // 2 执行 select
        return db.queryRows<T>(sql, params, getRecordTranformer<T>(T::class))
    }

    /**
     * 查找一个： select ... limit 1语句
     *  对 findAll(clazz:KClass<T>) 的精简版，直接根据泛型 T 来确定返回对象的类型
     *
     * @return object
     */
    public inline fun <reified T:Any> find(): T? {
        // 1 编译
        val (sql, params) = compile("select");

        // 2 执行 select
        return db.queryRow<T>(sql, params, getRecordTranformer<T>(T::class));
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