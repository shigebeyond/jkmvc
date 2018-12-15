package com.jkmvc.db

import kotlin.reflect.KClass

/**
 * sql查询执行
 *
 * 注：为什么不是接口，而是抽象类？
 *    因为我需要实现 inline fun <reified T:Any> find(): T? / inline fun <reified T:Any>  findAll(): List<T>
 *    这两个方法都需要具体化泛型，因此需要内联实现inline，但是inline不能用于接口方法/抽象方法，因此我直接在该类中实现这两个方法，该类也只能由接口变为抽象类
 */
abstract class IDbQuery{
    /**
     * 获得记录转换器
     * @param clazz 要转换的类
     * @return 转换的匿名函数
     */
    public open fun <T:Any> getRecordTranformer(clazz: KClass<T>): ((MutableMap<String, Any?>) -> T) {
        return clazz.recordTranformer
    }

    /**
     * 查找多个： select 语句
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @param transform 转换函数
     * @return 列表
     */
    public abstract fun <T:Any> findAll(vararg params: Any?, db: IDb = Db.instance(), transform:(MutableMap<String, Any?>) -> T): List<T>;

    /**
     * 查找多个： select 语句
     *  对 findAll(transform:(MutableMap<String, Any?>) 的精简版，直接根据泛型 T 来找到对应的记录转换器
     *  泛型 T 有3类情况，会生成不同的记录转换器
     *  1 Orm类：实例化并调用setOriginal()
     *  2 Map类: 直接返回记录数据，不用转换
     *  3 其他类：如果实现带 Map 参数的构造函数，如 constructor(data: MutableMap<String, Any?>)，就调用
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @return 列表
     */
    public inline fun <reified T:Any> findAll(vararg params: Any?, db: IDb = Db.instance()): List<T> {
        return findAll(*params, db = db, transform = getRecordTranformer<T>(T::class))
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @param transform 转换函数
     * @return 一个数据
     */
    public abstract fun <T:Any> find(vararg params: Any?,  db: IDb = Db.instance(), transform:(MutableMap<String, Any?>) -> T): T?;

    /**
     * 查找一个： select ... limit 1语句
     *  对 find(transform:(MutableMap<String, Any?>) 的精简版，直接根据泛型 T 来找到对应的记录转换器
     *  泛型 T 有3类情况，会生成不同的记录转换器
     *  1 Orm类：实例化并调用setOriginal()
     *  2 Map类: 直接返回记录数据，不用转换
     *  3 其他类：如果实现带 Map 参数的构造函数，如 constructor(data: MutableMap<String, Any?>)，就调用
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @return 一个数据
     */
    public inline fun <reified T:Any> find(vararg params: Any?, db: IDb = Db.instance()): T? {
        return find(*params, db = db, transform = getRecordTranformer<T>(T::class));
    }

    /**
     * 查询一列（多行）
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @return
     */
    public abstract fun findColumn(vararg params: Any?, db: IDb = Db.instance()): List<Any?>;

    /**
     * 查询一行一列
     *
     * @param params 动态参数
     * @param db 数据库连接
     * @return
     */
    public abstract fun findCell(vararg params: Any?, db: IDb = Db.instance()): Pair<Boolean, Any?>

    /**
     * 编译 + 执行
     *
     * @param action sql动作：select/insert/update/delete
     * @param params 动态参数
     * @param generatedColumn 返回自动生成的主键名
     * @param db 数据库连接
     * @return 影响行数|新增id
     */
    public abstract fun execute(action:SqlType, params:Array<out Any?>, generatedColumn:String? = null, db: IDb = Db.instance()):Int;

    /**
     * 批量更新有参数的sql
     *
     * @param action sql动作：select/insert/update/delete
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @param db 数据库连接
     * @return
     */
    public abstract fun batchExecute(action:SqlType, paramses: List<Any?>, paramSize:Int, db: IDb = Db.instance()): IntArray;
}