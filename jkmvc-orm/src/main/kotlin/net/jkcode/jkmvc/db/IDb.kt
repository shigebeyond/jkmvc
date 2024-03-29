package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.trySupplierFinally
import java.util.*
import kotlin.reflect.KClass

/**
 * 封装db操作
 *    为了保证IDb是接口，而非抽象类，将内联函数变为扩展函数，如 queryColumn()/queryValue() 因为要用到具体化泛型，因此才要内联inline
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
interface IDb: IDbMeta{

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
    fun begin();


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
     *    兼容 statement 返回类型是CompletableFuture
     *
     * @param statement db操作过程
     * @return
     */
    /**
     * 执行事务
     *    兼容 statement 返回类型是CompletableFuture
     *    TODO: 为优化性能, 可使用inline, 但是这样在调试时 statement 代码块中的变量不能在 idea 的 Debugger 窗口中的 variables 板块中直接看到
     *
     * @param statement db操作过程
     * @return
     */
    fun <T> transaction(statement: () -> T):T{
        begin(); // 开启事务

        return trySupplierFinally(statement){ r, ex ->
            if(ex != null){
                rollback(); // 回滚事务
                throw ex;
            }

            commit(); // 提交事务
            r
        }
    }

    /**
     * 执行事务
     *    TODO: 为优化性能, 可使用inline, 但是这样在调试时 statement 代码块中的变量不能在 idea 的 Debugger 窗口中的 variables 板块中直接看到
     *
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
     * 添加事务完成后的回调
     * @param callback 回调函数, 只有一个Boolean参数, 代表是否提交
     * @return
     */
    fun addTransactionCallback(callback: (Boolean)->Unit): IDb

    /**
     * 预览sql
     * @param sql
     * @param params sql参数
     * @return
     */
    fun previewSql(sql: String, params: List<*> = emptyList<Any>()): String

    /**
     * 执行更新
     * @param sql
     * @param params
     * @param generatedColumn 返回的自动生成的主键名
     * @return
     */
    fun execute(sql: String, params: List<*> = emptyList<Any>(), generatedColumn:String? = null): Long;

    /**
     * 执行更新, 并处理结果集
     *
     * @param sql
     * @param params
     * @param transform 结果转换函数
     * @return
     */
    fun <T> execute(sql: String, params: List<*> = emptyList<Any>(), transform: (DbResultSet) -> T): T?;

    /**
     * 批量更新: 每次更新sql参数不一样
     *
     * @param sql
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    fun batchExecute(sql: String, paramses: List<Any?>, paramSize:Int): IntArray;

    /************************* 查底层结果集, 要转换 ****************************/
    /**
     * 查询多行
     * @param sql
     * @param params
     * @param transform 结果转换函数
     * @return
     */
    fun <T> queryResult(sql: String, params: List<*> = emptyList<Any>(), transform: (DbResultSet) -> T): T;

    /**
     * 查询多行
     * @param sql
     * @param params 参数
     * @param result
     * @param transform 转换行的函数
     * @return
     */
    fun <T> queryRows(sql: String, params: List<*> = emptyList<Any>(), result: MutableList<T> = LinkedList<T>(), transform: (DbResultRow) -> T): List<T>{
        return queryResult(sql, params){ rs ->
            rs.mapTo(result, transform)
        }
    }

    /**
     * 查询一行(多列)
     *
     * @param sql
     * @param params 参数
     * @param transform 转换行的函数
     * @return
     */
    fun <T> queryRow(sql: String, params: List<*> = emptyList<Any>(), transform: (DbResultRow) -> T): T?{
        return queryResult(sql, params){ rs ->
            rs.firstOrNull()?.let { row ->
                transform(row)
            }
        }
    }

    /**
     * 查询一列(多行)
     *
     * @param sql
     * @param params 参数
     * @param clazz 值类型
     * @return
     */
    fun <T:Any> queryColumn(sql: String, params: List<*> = emptyList<Any>(), clazz: KClass<T>? = null): List<T>{
        return queryResult(sql, params){ rs ->
            rs.map{ row ->
                row.get(1, clazz) as T
            }
        }
    }

    /**
     * 查询一行一列
     *
     * @param sql
     * @param params 参数
     * @param clazz 值类型
     * @return
     */
    fun <T:Any> queryValue(sql: String, params: List<*> = emptyList<Any>(), clazz: KClass<T>? = null): T?{
        return queryResult(sql, params){ rs ->
            rs.firstOrNull()?.let { row ->
                row.get(1, clazz) as T?
            }
        }
    }

    /************************* 查高层对象 ****************************/
    /**
     * 查询多行
     * @param sql
     * @param params 参数
     * @param convertingColumn 是否转换字段名
     * @return
     */
    fun queryMaps(sql: String, params: List<*> = emptyList<Any>(), convertingColumn: Boolean = false): List<Map<String, Any?>>{
        return queryResult(sql, params){ rs ->
            rs.toMaps(convertingColumn)
        }
    }

    /**
     * 查询一行(多列)
     *
     * @param sql
     * @param params 参数
     * @param transform 转换行的函数
     * @param convertingColumn 是否转换字段名
     * @return
     */
    fun queryMap(sql: String, params: List<*> = emptyList<Any>(), convertingColumn: Boolean = false): Map<String, Any?>?{
        return queryRow(sql, params){ row ->
            row.toMap(convertingColumn)
        }
    }
}

/**
 * 查询一列(多行)
 *
 * @param sql
 * @param params 参数
 * @param clazz 值类型
 * @return
 */
public inline fun <reified T:Any> IDb.queryColumn(sql: String, params: List<*> = emptyList<Any>()): List<T>{
    return queryColumn(sql, params, T::class)
}

/**
 * 查询一行一列
 *
 * @param sql
 * @param params 参数
 * @return
 */
public inline fun <reified T:Any> IDb.queryValue(sql: String, params: List<*> = emptyList<Any>()): T? {
    return queryValue(sql, params, T::class)
}