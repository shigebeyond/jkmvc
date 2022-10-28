package net.jkcode.jkmvc.db.sharding

import net.jkcode.jkmvc.db.Db
import java.sql.Connection
import io.shardingjdbc.transaction.constants.SoftTransactionType
import io.shardingjdbc.transaction.bed.BEDSoftTransaction
import io.shardingjdbc.transaction.api.SoftTransactionManager
import io.shardingjdbc.transaction.api.config.SoftTransactionConfiguration
import io.shardingjdbc.transaction.bed.async.NestedBestEffortsDeliveryJob
import net.jkcode.jkmvc.db.single.DruidDataSourceFactory
import javax.sql.DataSource
import io.shardingjdbc.core.jdbc.core.ShardingContext
import net.jkcode.jkmvc.db.ClosableDataSource


/**
 * 分布式的db
 *    使用 sharding-jdbc 来获得连接
 *    由于sharding-jdbc自身已实现主从, 因此不用手动再实现主从, 直接 slaveConn = masterConn
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-08-23 10:31 AM
 */
class ShardingDb(name:String /* 标识 */) : Db(name) {

    companion object{

        /**
         * 事务日志的数据源
         */
        protected val transactionLogDataSource: ClosableDataSource
            get() = DruidDataSourceFactory.getDataSource("default.master");
    }

    /**
     * 连接使用情况
     */
    protected var connUsed: Boolean = false

    /**
     * 数据源
     */
    protected val dataSource: ClosableDataSource by lazy{
        ShardingDataSourceFactory.getDataSource(name)
    }

    /**
     * 主库连接
     */
    protected override val masterConn: Connection by lazy {
        connUsed = true
        dataSource.connection
    }

    /**
     * 随机一个从库连接
     */
    protected override val slaveConn: Connection
        get() = masterConn

    /**
     * 柔性事务
     */
    protected var transaction: BEDSoftTransaction? = null

    /**
     * 开启事务
     */
    protected override fun handleBegin(){
        // 1. 配置SoftTransactionConfiguration
        val config = SoftTransactionConfiguration(dataSource)
        // 使用默认的数据库作为事务日志的数据源
        config.transactionLogDataSource = transactionLogDataSource
        // 2. 初始化SoftTransactionManager
        val transactionManager = SoftTransactionManager(config)
        transactionManager.init()
        // 3. 获取BEDSoftTransaction
        transaction = transactionManager.getTransaction(SoftTransactionType.BestEffortsDelivery) as BEDSoftTransaction
        // 4. 开启事务
        transaction!!.begin(masterConn)
    }

    /**
     * 提交事务
     */
    protected override fun handleCommit(){
        // 关闭事务
        transaction!!.end()
        transaction = null
    }

    /**
     * 回滚事务
     */
    protected override fun handleRollback(){
        // 关闭事务
        transaction!!.end()
        transaction = null
    }

    /**
     * 关闭
     */
    public override fun close(){
        // 关闭连接
        if(connUsed)
            masterConn.close()

        closed = true
    }
}