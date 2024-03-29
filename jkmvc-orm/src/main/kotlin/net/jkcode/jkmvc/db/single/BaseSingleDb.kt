package net.jkcode.jkmvc.db.single

import net.jkcode.jkmvc.db.ClosableDataSource
import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.randomInt
import net.jkcode.jkmvc.db.Db
import net.jkcode.jkmvc.db.DbException
import net.jkcode.jkutil.common.dbLogger
import java.sql.Connection

/**
 * 单机的db
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-08-23 10:31 AM
 */
abstract class BaseSingleDb(name:String /* 标识 */) : Db(name) {

    /**
     * 数据库配置
     */
    protected val config: Config = Config.instance("dataSources.$name", "yaml")

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
     * 获得数据源
     * @param name 数据源名
     * @return
     */
    protected abstract fun getDataSource(name: String): ClosableDataSource

    /**
     * 主库连接
     */
    protected override val masterConn: Connection by lazy{
        //获得主库数据源
        val dataSource = getDataSource("$name.master");
        // 记录用到主库
        connUsed = connUsed or 1
        // 新建连接
        val conn = dataSource.connection
        if(conn.isClosed)
            throw DbException("Fail to get master connection for db [$name]")
        dbLogger.debug("Db [{}] create master connection: {}", name, conn)
        conn
    }

    /**
     * 随机一个从库连接
     */
    protected override val slaveConn: Connection by lazy {
        if (slaveNum == 0) { // 无从库, 直接用主库
            masterConn
        } else{ // 随机选个从库
            val i = randomInt(slaveNum)
            //获得从库数据源
            val dataSource = getDataSource("$name.slaves.$i");
            // 记录用到从库
            connUsed = connUsed or 2
            // 新建连接
            val conn = dataSource.connection
            if(conn.isClosed)
                throw DbException("Fail to get master connection for db [$name]")
            dbLogger.debug("Db [{}] create slave connection: {}", name, conn)
            conn
        }
    }

    /**
     * 记录事务开始时的 autoCommit
     */
    protected var preAutoCommit: Boolean = false

    /**
     * 开启事务
     */
    protected override fun handleBegin(){
        preAutoCommit = masterConn.autoCommit
        masterConn.autoCommit = false; // 禁止自动提交事务
    }

    /**
     * 提交事务
     */
    protected override fun handleCommit(){
        masterConn.commit()
        masterConn.autoCommit = preAutoCommit
        preAutoCommit = false
    }

    /**
     * 回滚事务
     */
    protected override fun handleRollback(){
        masterConn.rollback();
        masterConn.autoCommit = preAutoCommit
        preAutoCommit = false
    }

    /**
     * catalog
     */
    public override var catalog: String?
        get() = conn.catalog
        set(value){
            // 关闭主库连接
            if(connUsed and 1 > 0)
                masterConn.catalog = value
            // 关闭从库连接
            if(connUsed and 2 > 0)
                slaveConn.catalog = value
        }


    /**
     * 关闭
     */
    public override fun close(){
        // 关闭主库连接
        if((connUsed and 1) > 0) {
            dbLogger.debug("Db [{}] close master connection: {}", name, masterConn)
            masterConn.close()
        }
        // 关闭从库连接
        if((connUsed and 2) > 0
            && ((connUsed and 1) == 0 || masterConn != slaveConn)){ // 检查从库 != 主库, 防止重复关闭
            dbLogger.debug("Db [{}] close slave connection: {}", name, slaveConn)
            slaveConn.close()
        }
    }
}