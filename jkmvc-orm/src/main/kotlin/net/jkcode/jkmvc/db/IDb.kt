package net.jkcode.jkmvc.db

import kotlin.reflect.KClass

/**
 * 封装db操作
 *
 *  注：为什么不是接口，而是抽象类？
 *    因为我需要实现 inline public abstract fun <reified T:Any> queryValue(sql: String, params: List<Any?> = emptyList()): T?
 *    该方法都需要具体化泛型，因此需要内联实现inline，但是inline不能用于接口方法/抽象方法，因此我直接在该类中实现该方法，该类也只能由接口变为抽象类
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
abstract class IDb: IDbMeta, IDbValueQuoter, IDbIdentifierQuoter{

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
     *    兼容 statement 返回类型是CompletableFuture
     *
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
     * 添加事务完成后的回调
     * @param callback 回调函数, 只有一个Boolean参数, 代表是否提交
     * @return
     */
    public abstract fun addTransactionCallback(callback: (Boolean)->Unit): IDb

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
    public abstract fun execute(sql: String, params: List<Any?> = emptyList(), generatedColumn:String? = null): Long;

    /**
     * 批量更新: 每次更新sql参数不一样
     *
     * @param sql
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    public abstract fun batchExecute(sql: String, paramses: List<Any?>, paramSize:Int): IntArray;

    /************************* 查底层结果集, 要转换 ****************************/
    /**
     * 查询多行
     * @param sql
     * @param params
     * @param transform 结果转换函数
     * @return
     */
    public abstract fun <T> queryResult(sql: String, params: List<Any?> = emptyList(), transform: (DbResultSet) -> T): T;

    /**
     * 查询多行
     * @param sql
     * @param params 参数
     * @param transform 转换行的函数
     * @return
     */
    public fun <T> queryRows(sql: String, params: List<Any?> = emptyList(), transform: (DbResultRow) -> T): List<T>{
        return queryResult(sql, params){ rs ->
            rs.mapRows(transform)
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
    public fun <T> queryRow(sql: String, params: List<Any?> = emptyList(), transform: (DbResultRow) -> T): T?{
        return queryResult(sql, params){ rs ->
            rs.mapRow(transform)
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
    public fun <T:Any> queryColumn(sql: String, params: List<Any?> = emptyList(), clazz: KClass<T>? = null): List<T>{
        return queryResult(sql, params){ rs ->
            rs.mapRows{ row ->
                row.get(1, clazz) as T
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
    public inline fun <reified T:Any> queryColumn(sql: String, params: List<Any?> = emptyList()): List<T>{
        return queryColumn(sql, params, T::class)
    }

    /**
     * 查询一行一列
     *
     * @param sql
     * @param params 参数
     * @param clazz 值类型
     * @return
     */
    public fun <T:Any> queryValue(sql: String, params: List<Any?> = emptyList(), clazz: KClass<T>? = null): T?{
        return queryResult(sql, params){ rs ->
            rs.mapRow{ row ->
                row.get(1, clazz) as T?
            }
        }
    }

    /**
     * 查询一行一列
     *
     * @param sql
     * @param params 参数
     * @return
     */
    public inline fun <reified T:Any> queryValue(sql: String, params: List<Any?> = emptyList()): T? {
        return queryValue(sql, params, T::class)
    }

    /************************* 查高层对象 ****************************/
    /**
     * 查询多行
     * @param sql
     * @param params 参数
     * @param convertingColumn 是否转换字段名
     * @return
     */
    public fun queryMaps(sql: String, params: List<Any?> = emptyList(), convertingColumn: Boolean = false): List<Map<String, Any?>>{
        val columnTransform: ((String)->String)? = if(convertingColumn) this::column2Prop else null
        return queryResult(sql, params){ rs ->
            rs.toMaps(columnTransform)
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
    public fun queryMap(sql: String, params: List<Any?> = emptyList(), convertingColumn: Boolean = false): Map<String, Any?>?{
        val columnTransform: ((String)->String)? = if(convertingColumn) this::column2Prop else null
        return queryRow(sql, params){ row ->
            row.toMap(columnTransform)
        }
    }

}