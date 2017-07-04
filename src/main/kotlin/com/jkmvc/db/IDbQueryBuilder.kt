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
abstract class IDbQueryBuilder:IDbQueryBuilderAction, IDbQueryBuilderDecoration, Cloneable {
    /**
     * 获得记录转换器
     */
    public abstract fun <T:Any> getRecordTranformer(clazz: KClass<T>): ((MutableMap<String, Any?>) -> T);

    /**
     * 设置是否预编译参数化的sql
     *
     * @param prepared 是否预编译sql
     * @return IDbQueryBuilder
     */
    public abstract fun prepare(prepared:Boolean = true):IDbQueryBuilder;

    /**
     * 编译sql
     *
     * @param action sql动作：select/insert/update/delete
     * @return 编译结果(sql+参数)
     */
    public abstract fun compile(action:ActionType): CompiledSql;

    /**
     * 查找多个： select 语句
     *
     * @param params 动态参数
     * @param fun transform 转换函数
     * @return 列表
     */
    public abstract fun <T:Any> findAll(vararg params: Any?, transform:(MutableMap<String, Any?>) -> T): List<T>;

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 动态参数
     * @param fun transform 转换函数
     * @return 一个数据
     */
    public abstract fun <T:Any> find(vararg params: Any?, transform:(MutableMap<String, Any?>) -> T): T?;

    /**
     * 查找多个： select 语句
     *  对 findAll(transform:(MutableMap<String, Any?>) 的精简版，直接根据泛型 T 来找到其构造函数来创建对象
     *  泛型 T 必须实现带 Map 参数的构造函数，如 constructor(data: MutableMap<String, Any?>)
     *
     * @param params 动态参数
     * @return 列表
     */
    public inline fun <reified T:Any> findAll(vararg params: Any?): List<T> {
        // 1 编译
        val result = compile(ActionType.SELECT);

        // 2 执行 select
        return db.queryRows<T>(result.sql, result.buildParams(params), getRecordTranformer<T>(T::class))
    }

    /**
     * 查找一个： select ... limit 1语句
     *  对 find(transform:(MutableMap<String, Any?>) 的精简版，直接根据泛型 T 来找到其构造函数来创建对象
     *  泛型 T 必须实现带 Map 参数的构造函数，如 constructor(data: MutableMap<String, Any?>)
     *
     * @param params 动态参数
     * @return 一个数据
     */
    public inline fun <reified T:Any> find(vararg params: Any?): T? {
        // 1 编译
        val result = limit(1).compile(ActionType.SELECT);

        // 2 执行 select
        return db.queryRow<T>(result.sql, result.buildParams(params), getRecordTranformer<T>(T::class));
    }

    /**
     * 编译 + 执行
     *
     * @param action sql动作：select/insert/update/delete
     * @param params 动态参数
     * @param returnGeneratedKey 是否返回自动生成的主键
     * @return 影响行数|新增id
     */
    public abstract fun execute(action:ActionType, params:Array<out Any?>, returnGeneratedKey:Boolean = false):Int;

    /**
     * 批量更新有参数的sql
     *
     * @param action sql动作：select/insert/update/delete
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    public abstract fun batchExecute(action:ActionType, paramses: List<Any?>, paramSize:Int): IntArray;

    /**
     * 统计行数： count语句
     *
     * @param params 动态参数
     * @return
     */
    public abstract fun count(vararg params: Any?):Long;

    /**
     * 插入：insert语句
     *
     * @param returnGeneratedKey 是否返回自动生成的主键
     * @param params 动态参数
     * @return 影响行数|新增的id
     */
    public abstract fun insert(returnGeneratedKey:Boolean = false, vararg params: Any?):Int;

    /**
     * 更新：update语句
     *
     * @param params 动态参数
     * @return
     */
    public abstract fun update(vararg params: Any?):Boolean;

    /**
     * 删除
     *
     * @param params 动态参数
     * @return
     */
    public abstract fun delete(vararg params: Any?):Boolean;

    /**
     * 克隆对象: 单纯用于改权限为public
     * 
     * @return o
     */
    public override fun clone(): Any{
        return super.clone()
    }
}