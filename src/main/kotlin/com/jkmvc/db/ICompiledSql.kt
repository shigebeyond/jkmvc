package com.jkmvc.db

import java.util.*

/**
 * 编译好的sql
 *
 * @author shijianhang
 * @date 2017-6-10 下午8:02:47
 */
interface ICompiledSql {

    /****************************** 构建参数/sql *******************************/
    /**
     * 数据库名
     */
    val dbName:String

    /**
     * 编译好的sql
     */
    var sql: String

    /**
     * 编译后的sql参数 / 静态参数
     */
    var staticParams: LinkedList<Any?>

    /**
     * 动态参数的个数 = 静态参数中?的个数
     */
    val dynamicParamsSize:Int;

    /**
     * 数据库连接
     *   由于 CompiledSql 作为编译后的sql可以被缓存起来，以便复用，避免重复编译sql，因此一个 CompiledSql 对象可以跨越多个请求/线程而存在的，从而导致他的属性不能有只存在于单个请求/线程的对象，如数据库连接（上一个请求与下一个请求获得的连接可以不一样）
     *   => 只记录dbName，每次执行都获得当前线程最新的连接
     */
    val db: IDb

    /**
     * 判断是否为空
     * @return
     */
    fun isEmpty(): Boolean

    /**
     * 清空编译结果
     * @return
     */
    fun clear(): ICompiledSql

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param params 动态参数
     * @return
     */
    fun buildParams(dynamicParams: List<Any?> = emptyList()): List<Any?>

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param params 动态参数
     * @return
     */
    fun buildParams(dynamicParams: Array<out Any?>): List<Any?>{
        return buildParams(dynamicParams.asList())
    }

    /**
     * 构建实际参数 = 静态参数 + 动态参数
     *
     * @param dynamicParamses 多次处理的动态参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    fun buildBatchParamses(dynamicParamses: List<Any?>, paramSize: Int):List<Any?>;

    /**
     * 预览sql
     *
     * @param params 动态参数
     * @param fromIndex 动态参数的开始索引
     * @return
     */
    fun previewSql(dynamicParams:List<Any?> = emptyList(), fromIndex:Int = 0): String

    /****************************** 执行与查询sql *******************************/
    /**
     * 查找多个记录： select 语句
     *
     * @param params 动态参数
     * @param fun transform 转换函数
     * @return 列表
     */
    fun <T:Any> findAll(vararg params: Any?, transform:(MutableMap<String, Any?>) -> T): List<T>;

    /**
     * 查找一个记录： select ... limit 1语句
     *
     * @param params 动态参数
     * @param fun transform 转换函数
     * @return 一个数据
     */
    fun <T:Any> find(vararg params: Any?, transform:(MutableMap<String, Any?>) -> T): T?;

    /**
     * 查询一个值（单行单列）
     *
     * @param params 动态参数
     * @return
     */
    fun findInt(vararg params: Any?):Int

    /**
     * 查询一个值（单行单列）
     *
     * @param params 动态参数
     * @return
     */
    fun findLong(vararg params: Any?):Long

    /**
     * 查询一个值（单行单列）
     *
     * @param params 动态参数
     * @return
     */
    fun findFloat(vararg params: Any?):Float

    /**
     * 查询一个值（单行单列）
     *
     * @param params 动态参数
     * @return
     */
    fun findDouble(vararg params: Any?):Double

    /**
     * 查询一个值（单行单列）
     *
     * @param params 动态参数
     * @return
     */
    fun findBoolean(vararg params: Any?):Boolean

    /**
     * 查询一个值（单行单列）
     *
     * @param params 动态参数
     * @return
     */
    fun findByte(vararg params: Any?):Byte

    /**
     * 查询一个值（单行单列）
     *
     * @param params 动态参数
     * @return
     */
    fun findShort(vararg params: Any?):Short

    /**
     * 编译 + 执行
     *
     * @param params 动态参数
     * @param returnGeneratedKey 是否返回自动生成的主键
     * @return 影响行数|新增id
     */
    fun execute(params:Array<out Any?>, returnGeneratedKey:Boolean = false):Int;

    /**
     * 批量更新有参数的sql
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    fun batchExecute(paramses: List<Any?>, paramSize:Int): IntArray;
}