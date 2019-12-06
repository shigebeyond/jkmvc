package net.jkcode.jkmvc.db

import net.jkcode.jkutil.common.dbLogger
import net.jkcode.jkutil.common.mapToArray
import net.jkcode.jkutil.common.trySupplierFinally
import net.jkcode.jkmvc.db.sharding.ShardingDb
import net.jkcode.jkmvc.db.single.SingleDb
import net.jkcode.jkutil.ttl.AllRequestScopedTransferableThreadLocal
import java.io.Closeable
import java.sql.Connection
import java.util.*

/**
 * 封装db操作
 *   1. ThreadLocal保证的线程安全, 每个请求都创建新的db对象, 请求结束要调用 close() 来关闭连接
 *   2. db对象的生命周期是请求级
 *   3. 继承 Closeable 来关闭连接
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
abstract class Db protected constructor(
        public override val name:String /* 标识 */,
        public override val dbMeta: IDbMeta = DbMeta.get(name) /* 元数据 */
) : IDb(), IDbMeta by dbMeta, Closeable {

    companion object {

        /**
         * 线程安全的db缓存
         *    每个线程有多个db, 一个名称各一个db对象
         *    每个请求都创建新的db对象, 请求结束要调用 close() 来关闭连接
         */
        protected val dbs: AllRequestScopedTransferableThreadLocal<HashMap<String, Db>> = object: AllRequestScopedTransferableThreadLocal<HashMap<String, Db>>({HashMap()}){
            public override fun endScope() {
                // 请求结束要调用 close() 来关闭连接
                val dbs = get()
                for((name, db) in dbs)
                    db.close()
                dbs.clear()

                super.endScope()
            }
        }

        /**
         * 获得db(线程安全)
         *    获得当前线程下的指定名字的db, 没有则创建新db
         *    每个请求都创建新的db对象, 请求结束要调用 close() 来关闭连接
         * @param name
         * @return
         */
        public fun instance(name: String = "default"):Db{
            return dbs.get().getOrPut(name){
                // 是否分库
                if(DbConfig.isSharding(name))
                    ShardingDb(name)
                else
                    SingleDb(name)
            }
        }

    }

    /**
     * 主库连接
     */
    protected abstract val masterConn: Connection

    /**
     * 随机一个从库连接
     */
    protected abstract val slaveConn: Connection

    /**
     * 是否强制使用主库
     *    db对象的生命周期是请求级
     */
    public var forceMaster: Boolean = false;

    /**
     * 获得通用的连接
     *   如果有事务+强制, 就使用主库
     *   否则使用从库
     */
    public val conn: Connection
        get(){
            // 不能使用`if(isInTransaction()) masterConn else slaveConn`, 否则会创建2个连接
            if(isInTransaction() || forceMaster)
                return masterConn

            return slaveConn
        }

    /**
     * 当前事务的嵌套层级
     */
    protected var transDepth:Int = 0;

    /**
     * 标记当前事务是否回滚
     */
    protected var rollbacked = false;

    /**
     * 事务完成后的回调
     */
    protected val transactionCallbacks: MutableList<(Boolean)->Unit> by lazy{
        LinkedList<(Boolean)->Unit>()
    }

    /**
     * 是否强制使用主库
     */
    public override fun forceMaster(f: Boolean): IDb{
        this.forceMaster = f
        return this
    }

    /**
     * 执行事务
     *    兼容 statement 返回类型是CompletableFuture
     *
     * @param statement db操作过程
     * @return
     */
    public override fun <T> transaction(statement: () -> T):T{
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
     * 添加事务完成后的回调
     * @param callback 回调函数, 只有一个Boolean参数, 代表是否提交
     * @return
     */
    public override fun addTransactionCallback(callback: (Boolean)->Unit): IDb {
        transactionCallbacks.add(callback)
        return this
    }

    /**
     * 处理开启事务
     */
    protected abstract fun handleBegin()

    /**
     * 处理提交事务
     */
    protected abstract fun handleCommit()

    /**
     * 处理回滚事务
     */
    protected abstract fun handleRollback()

    /**
     * 开启事务
     */
    public override fun begin(){
        if(transDepth++ === 0)
            handleBegin()
    }

    /**
     * 提交事务
     */
    public override fun commit(): Boolean{
        // 未开启事务
        if (transDepth <= 0)
            return false;

        // 无嵌套事务
        if (--transDepth === 0)
        {
            // 回滚 or 提交事务: 回滚的话,返回false
            if(rollbacked)
                handleRollback()
            else
                handleCommit()
            val result = !rollbacked;
            // 调用回调
            transactionCallbacks.forEach {
                it.invoke(result)
            }
            rollbacked = false; // 清空回滚标记
            return result;
        }

        // 有嵌套事务
        return true;
    }

    /**
     * 回滚事务
     */
    public override fun rollback():Boolean{
        // 未开启事务
        if (transDepth <= 0)
            return false;

        // 无嵌套事务
        if (--transDepth === 0)
        {
            rollbacked = false; // 清空回滚标记
            handleRollback() // 回滚事务
            // 调用回调
            transactionCallbacks.forEach {
                it.invoke(false)
            }
        }

        // 有嵌套事务
        rollbacked = true; // 标记回滚
        return true;
    }

    /**
     * 是否在事务中
     * @return
     */
    public override fun isInTransaction(): Boolean {
        return transDepth > 0;
    }

    /**
     * 预览sql
     *
     * @param sql
     * @param params sql参数
     * @return
     */
    public override fun previewSql(sql: String, params: List<Any?>): String {
        // 1 无参数
        if(params.isEmpty())
            return sql

        // 2 有参数：替换参数
        // 正则替换
        /*var i = 0 // 迭代索引
        return sql.replace("\\?".toRegex()) { matches: MatchResult ->
            quote(params[i++]) // 转义参数值
        }*/

        // 格式化字符串
        val ps = params.mapToArray {
            quote(it)
        }
        return sql.replace("?", "%s").format(*ps)
    }

    /**
     * 执行更新
     *
     * @param sql
     * @param params
     * @param generatedColumn 返回的自动生成的主键名
     * @return
     */
    public override fun execute(sql: String, params: List<Any?>, generatedColumn:String?): Long {
        try{
            return masterConn.execute(sql, params, generatedColumn);
        }catch (e:Exception){
            dbLogger.error("出错[{}] sql: {}", e.message, previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 批量更新：每次更新sql参数不一样
     *
     * @param sql
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     * @return
     */
    public override fun batchExecute(sql: String, paramses: List<Any?>, paramSize:Int): IntArray {
        try{
            return masterConn.batchExecute(sql, paramses, paramSize)
        }catch (e:Exception){
            dbLogger.error("出错[{}], sql={}, params={}", e.message, sql, paramses)
            throw  e
        }
    }

    /**
     * 查询多行
     *
     * @param sql
     * @param params
     * @param transform 结果转换函数
     * @return
     */
    public override fun <T> queryResult(sql: String, params: List<Any?>, transform: (DbResultSet) -> T): T {
        try{
            return conn.queryResult(sql, params){
                transform(DbResultSet(this, it))
            }
        }catch (e:Exception){
            dbLogger.error("出错[{}] sql: {}", e.message, previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 关闭连接+清理ThreadLocal
     *   只提供给第三方框架调用, 因为他们没有请求作用域, 无法自动清理ThreadLocal
     */
    public fun closeAndClear(){
        // 关闭连接
        close()
        // 清理ThreadLocal
        dbs.get().remove(name)
    }
}
