package com.jkmvc.db

import com.jkmvc.closing.ClosingOnRequestEnd
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * 封装db操作
 *
 *  注：为什么不是接口，而是抽象类？
 *    因为我需要实现 inline public abstract fun <reified T:Any> queryCell(sql: String, params: List<Any?> = emptyList()): Cell<T>
 *    该方法都需要具体化泛型，因此需要内联实现inline，但是inline不能用于接口方法/抽象方法，因此我直接在该类中实现该方法，该类也只能由接口变为抽象类
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
abstract class IDb: IDbMeta, IDbValueQuoter, IDbIdentifierQuoter, ClosingOnRequestEnd() {

    /**
     * db元数据
     */
    public abstract val dbMeta: IDbMeta;

    /**
     * 是否强制使用主库
     */
    public abstract fun forceMaster(f: Boolean): IDb

    /***************************** 执行sql ******************************/
    /**
     * 开启事务
     */
    public abstract fun begin():Unit;


    /**
     * 提交
     */
    public abstract fun commit():Boolean;

    /**
     * 回滚
     */
    public abstract fun rollback():Boolean;

    /**
     * 执行事务
     * @param statement db操作过程
     * @return
     */
    public abstract fun <T> transaction(statement: () -> T):T;

    /**
     * 执行事务
     * @param fake 不真正使用事务
     * @param statement db操作过程
     * @return
     */
    public fun <T> transaction(fake: Boolean, statement: () -> T):T{
        if(fake)
            return statement()

        return transaction(statement)
    }

    /**
     * 是否在事务中
     * @return
     */
    public abstract fun isInTransaction(): Boolean

    /**
     * 预览sql
     * @param sql
     * @param params sql参数
     * @return
     */
    public abstract fun previewSql(sql: String, params: List<Any?> = emptyList()): String

    /**
     * 执行更新
     * @param sql
     * @param params
     * @param generatedColumn 返回的自动生成的主键名
     * @return
     */
    public abstract fun execute(sql: String, params: List<Any?> = emptyList(), generatedColumn:String? = null): Int;

    /**
     * 批量更新: 每次更新sql参数不一样
     *
     * @param sql
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    public abstract fun batchExecute(sql: String, paramses: List<Any?>, paramSize:Int): IntArray;

    /***************************** 查询 ******************************/
    /**
     * 查询多行
     * @param sql
     * @param params
     * @param action 转换结果的函数
     * @return
     */
    public abstract fun <T> queryResult(sql: String, params: List<Any?> = emptyList(), action: (ResultSet) -> T): T;

    /**
     * 查询多行
     * @param sql
     * @param params 参数
     * @param transform 转换结果的函数
     * @return
     */
    public abstract fun <T> queryRows(sql: String, params: List<Any?> = emptyList(), transform: (Row) -> T): List<T>;

    /**
     * 查询一行(多列)
     *
     * @param sql
     * @param params 参数
     * @param transform 转换结果的函数
     * @return
     */
    public abstract fun <T> queryRow(sql: String, params: List<Any?> = emptyList(), transform: (Row) -> T): T?;

    /**
     * 查询一列(多行)
     *
     * @param sql
     * @param params 参数
     * @param clazz 值类型
     * @return
     */
    public abstract fun <T:Any> queryColumn(sql: String, params: List<Any?> = emptyList(), clazz: KClass<T>?): List<T?>

    /**
     * 查询一行一列
     *
     * @param sql
     * @param params 参数
     * @param clazz 值类型
     * @return
     */
    public abstract fun <T:Any> queryCell(sql: String, params: List<Any?> = emptyList(), clazz: KClass<T>?): Cell<T>

    /**
     * 查询一行一列
     *
     * @param sql
     * @param params 参数
     * @return
     */
    public inline fun <reified T:Any> queryCell(sql: String, params: List<Any?> = emptyList()): Cell<T> {
        return queryCell(sql, params, T::class)
    }
}