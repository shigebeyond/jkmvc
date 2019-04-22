package net.jkcode.jkmvc.db

import net.jkcode.jkmvc.common.*
import java.io.Closeable
import java.sql.Connection
import java.sql.ResultSet
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.reflect.KClass

/**
 * 封装db操作
 *   ThreadLocal保证的线程安全, 每个请求都创建新的db对象
 *   db对象的生命周期是请求级
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
class Db protected constructor(public override val name:String /* 标识 */,
                               public override val dbMeta: IDbMeta = DbMeta.get(name) /* 元数据 */
) : IDb(), IDbMeta by dbMeta {

    companion object {

        /**
         * 公共配置
         */
        public val commonConfig: Config = Config.instance("database.__common", "yaml")

        /**
         * 是否调试
         */
        public val debug:Boolean = commonConfig.getBoolean("debug", false)!!;

        /**
         * 数据源工厂
         */
        public val dataSourceFactory:IDataSourceFactory by lazy{
            val clazz:String = commonConfig["dataSourceFactory"]!!
            Class.forName(clazz).newInstance() as IDataSourceFactory
        }

        /**
         * 线程安全的db缓存
         *    每个线程有多个db, 一个名称各一个db对象
         *    每个请求都创建新的db对象, 请求结束要调用 close() 来删除db对象
         */
        protected val dbs:ThreadLocal<HashMap<String, Db>> = ThreadLocal.withInitial {
            HashMap<String, Db>();
        }

        /**
         * 获得db(线程安全)
         *    获得当前线程下的指定名字的db, 没有则创建新db
         *    每个请求都创建新的db对象, 请求结束要调用 close() 来删除db对象
         * @param name
         * @return
         */
        public fun instance(name:String = "default"):Db{
            return dbs.get().getOrPut(name){
                Db(name);
            }
        }

    }

    /**
     * 是否强制使用主库
     *    db对象的生命周期是请求级
     */
    public var forceMaster: Boolean = false;

    /**
     * 数据库配置
     */
    protected val config: Config = Config.instance("database.$name", "yaml")

    /**
     * 从库数量
     */
    protected val slaveNum: Int by lazy{
        val slaves = config.getList("slaves")
        if(slaves == null) 0 else slaves.size
    }

    /**
     * 连接使用情况
     *   用2位bit来记录是否用到主从连接
     *   主库第0位, 从库第1位
     */
    protected var connUsed: Int = 0

    /**
     * 主库连接
     */
    protected val masterConn: Connection by lazy{
        //获得主库数据源
        val dataSource = dataSourceFactory.getDataSource("$name.master");
        // 记录用到主库
        connUsed = connUsed or 1
        // 新建连接
        dataSource.connection
    }

    /**
     * 随机一个从库连接
     */
    protected val slaveConn: Connection by lazy {
        if (slaveNum == 0) { // 无从库, 直接用主库
            masterConn
        } else{ // 随机选个从库
            val i = randomInt(slaveNum)
            //获得从库数据源
            val dataSource = dataSourceFactory.getDataSource("$name.slaves.$i");
            // 记录用到从库
            connUsed = connUsed or 2
            // 新建连接
            dataSource.connection
        }
    }

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
     * 是否强制使用主库
     */
    public override fun forceMaster(f: Boolean): IDb{
        this.forceMaster = f
        return this
    }

    /**
     * 执行事务
     * @param statement db操作过程
     * @return
     */
    public override fun <T> transaction(statement: () -> T):T{
        try{
            begin(); // 开启事务
            val result:T = statement(); // 执行sql
            commit(); // 提交事务
            return result; // 返回结果
        }catch(e:Exception){
            rollback(); // 回顾
            throw e;
        }
    }

    /**
     * 开启事务
     */
    public override fun begin():Unit{
        if(transDepth++ === 0)
            masterConn.autoCommit = false; // 禁止自动提交事务
    }

    /**
     * 提交事务
     */
    public override fun commit():Boolean{
        // 未开启事务
        if (transDepth <= 0)
            return false;

        // 无嵌套事务
        if (--transDepth === 0)
        {
            // 回滚 or 提交事务: 回滚的话,返回false
            if(rollbacked)
                masterConn.rollback();
            else
                masterConn.commit()
            val result = rollbacked;
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
            masterConn.rollback(); // 回滚事务
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
    public override fun execute(sql: String, params: List<Any?>, generatedColumn:String?): Int {
        try{
            // forceMaster = true // 更新后, 让后续操作走主库
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
            // forceMaster = true // 更新后, 让后续操作走主库
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
     * @param action 转换结果的函数
     * @return
     */
    public override fun <T> queryResult(sql: String, params: List<Any?>, action: (ResultSet) -> T): T {
        try{
            return conn.queryResult(sql, params, action)
        }catch (e:Exception){
            dbLogger.error("出错[{}] sql: {}", e.message, previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 查询一行(多列)
     *
     * @param sql
     * @param params
     * @param transform 转换结果的函数
     * @return
     */
    public override fun <T> queryRow(sql: String, params: List<Any?>, transform: (Row) -> T): T? {
        try{
            return conn.queryRow(sql, params, transform);
        }catch (e:Exception){
            dbLogger.error("出错[{}] sql: {}", e.message, previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 查询多行
     *
     * @param sql
     * @param params
     * @param transform 转换结果的函数
     * @return
     */
    public override fun <T> queryRows(sql: String, params: List<Any?>, transform: (Row) -> T): List<T> {
        try{
            return conn.queryRows(sql, params, transform);
        }catch (e:Exception){
            dbLogger.error("出错[{}] sql: {}", e.message, previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 查询一列(多行)
     *
     * @param sql
     * @param params
     * @param clazz 值类型
     * @return
     */
    public override fun <T:Any> queryColumn(sql: String, params: List<Any?>, clazz: KClass<T>?): List<T?> {
        try{
            return conn.queryColumn(sql, params);
        }catch (e:Exception){
            dbLogger.error("出错[{}] sql: {}", e.message, previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 查询一行一列
     *
     * @param sql
     * @param params
     * @param clazz 值类型
     * @return
     */
    public override fun <T:Any> queryCell(sql: String, params: List<Any?>, clazz: KClass<T>?): Cell<T> {
        try{
            return conn.queryCell(sql, params, clazz);
        }catch (e:Exception){
            dbLogger.error("出错[{}] sql: {}", e.message, previewSql(sql, params))
            throw  e
        }
    }

    /**
     * 关闭
     */
    public override fun close():Unit{
        // 删除当前线程的db对象
        if(dbs.get().remove(name) == null)
            return // 当前线程并没有db对象

        // 关闭连接
        if(connUsed and 1 > 0)
            masterConn.close()
        if(connUsed and 2 > 0)
            slaveConn.close()
    }
}
