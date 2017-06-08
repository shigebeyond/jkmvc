package com.jkmvc.db

import kotlin.reflect.KClass

/**
 * sql构建器
 *   依次继承 DbQueryBuilderAction 处理动作子句 + DbQueryBuilderDecoration 处理修饰子句
 *   提供select/where等类sql的方法, 但是调用方法时, 不直接拼接sql, 而是在compile()时才延迟拼接sql, 因为调用方法时元素可以无序, 但生成sql时元素必须有序
 *
 * 注：为什么不是接口，而是抽象类？
 *    因为我需要实现 inline fun <reified T:Any> find(): T? / inline fun <reified T:Any>  findAll(): List<T>
 *    这两个方法都需要具体化泛型，因此需要内联实现inline，但是inline不能用于接口方法/抽象方法，因此我直接在该类中实现这两个方法，该类也只能由接口变为抽象类
 *
 * @author shijianhang
 * @date 2016-10-13
 */
abstract class IDbQueryBuilder:IDbQueryBuilderAction, IDbQueryBuilderDecoration {
    /**
     * 获得记录转换器
     */
    public abstract fun <T:Any> getRecordTranformer(clazz: KClass<T>): ((MutableMap<String, Any?>) -> T);

    /**
     * 编译sql
     *
     * @param action sql动作：select/insert/update/delete
     * @return Pair(sql, 参数)
     */
    public abstract fun compile(action:String): Pair<String, List<Any?>>;

    /**
     * 查找多个： select 语句
     *
     * @param fun transform 转换函数
     * @return 列表
     */
    public abstract fun <T:Any> findAll(transform:(MutableMap<String, Any?>) -> T): List<T>;

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param fun transform 转换函数
     * @return 一个数据
     */
    public abstract fun <T:Any> find(transform:(MutableMap<String, Any?>) -> T): T?;

    /**
     * 查找多个： select 语句
     *  对 findAll(transform:(MutableMap<String, Any?>) 的精简版，直接根据泛型 T 来找到其构造函数来创建对象
     *  泛型 T 必须实现带 Map 参数的构造函数，如 constructor(data: MutableMap<String, Any?>)
     *
     * @return 列表
     */
    public inline fun <reified T:Any> findAll(): List<T> {
        // 1 编译
        val (sql, params) = compile("select");

        // 2 执行 select
        return db.queryRows<T>(sql, params, getRecordTranformer<T>(T::class))
    }

    /**
     * 查找一个： select ... limit 1语句
     *  对 find(transform:(MutableMap<String, Any?>) 的精简版，直接根据泛型 T 来找到其构造函数来创建对象
     *  泛型 T 必须实现带 Map 参数的构造函数，如 constructor(data: MutableMap<String, Any?>)
     *
     * @return 一个数据
     */
    public inline fun <reified T:Any> find(): T? {
        // 1 编译
        val (sql, params) = limit(1).compile("select");

        // 2 执行 select
        return db.queryRow<T>(sql, params, getRecordTranformer<T>(T::class));
    }

    /**
     * 统计行数： count语句
     * @return
     */
    public abstract fun count():Long;

    /**
     * 插入：insert语句
     * @return 新增的id
     */
    public abstract fun insert():Int;

    /**
     *	更新：update语句
     *	@return
     */
    public abstract fun update():Boolean;

    /**
     *	删除
     *	@return	bool
     */
    public abstract fun delete():Boolean;
}