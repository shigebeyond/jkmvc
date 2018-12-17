package com.jkmvc.db

import java.io.Closeable
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * 封装db操作
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
interface IDb: Closeable, IDbMeta, IDbValueQuoter, IDbIdentifierQuoter {

    /**
     * db元数据
     */
    val dbMeta: IDbMeta;

    /**
     * 是否强制使用主库
     */
    fun forceMaster(f: Boolean): IDb

    /***************************** 执行sql ******************************/
    /**
     * 开启事务
     */
    fun begin():Unit;


    /**
     * 提交
     */
    fun commit():Boolean;

    /**
     * 回滚
     */
    fun rollback():Boolean;

    /**
     * 执行事务
     * @param statement db操作过程
     * @return
     */
    fun <T> transaction(statement: () -> T):T;

    /**
     * 执行事务
     * @param fake 不真正使用事务
     * @param statement db操作过程
     * @return
     */
    fun <T> transaction(fake: Boolean, statement: () -> T):T{
        if(fake)
            return statement()

        return transaction(statement)
    }

    /**
     * 是否在事务中
     * @return
     */
    fun isInTransaction(): Boolean

    /**
     * 预览sql
     * @param sql
     * @param params sql参数
     * @return
     */
    fun previewSql(sql: String, params: List<Any?> = emptyList()): String

    /**
     * 执行更新
     * @param sql
     * @param params
     * @param generatedColumn 返回的自动生成的主键名
     * @return
     */
    fun execute(sql: String, params: List<Any?> = emptyList(), generatedColumn:String? = null): Int;

    /**
     * 批量更新: 每次更新sql参数不一样
     *
     * @param sql
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    fun batchExecute(sql: String, paramses: List<Any?>, paramSize:Int): IntArray;

    /***************************** 查询 ******************************/
    /**
     * 查询多行
     * @param sql
     * @param params
     * @param action 转换结果的函数
     * @return
     */
    fun <T> queryResult(sql: String, params: List<Any?> = emptyList(), action: (ResultSet) -> T): T;

    /**
     * 查询多行
     * @param sql
     * @param params
     * @param transform 转换结果的函数
     * @return
     */
    fun <T> queryRows(sql: String, params: List<Any?> = emptyList(), transform: (MutableMap<String, Any?>) -> T): List<T>;

    /**
     * 查询一行(多列)
     *
     * @param sql
     * @param params
     * @param transform 转换结果的函数
     * @return
     */
    fun <T> queryRow(sql: String, params: List<Any?> = emptyList(), transform: (MutableMap<String, Any?>) -> T): T?;

    /**
     * 查询一列(多行)
     *
     * @param sql
     * @param params
     * @param clazz 值类型
     * @return
     */
    fun <T:Any> queryColumn(sql: String, params: List<Any?> = emptyList(), clazz: KClass<T>?): List<T?>

    /**
     * 查询一行一列
     *
     * @param sql
     * @param params
     * @param clazz 值类型
     * @return
     */
    fun <T:Any> queryCell(sql: String, params: List<Any?> = emptyList(), clazz: KClass<T>?): Cell<T>
}