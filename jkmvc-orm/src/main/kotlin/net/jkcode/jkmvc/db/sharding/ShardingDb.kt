package net.jkcode.jkmvc.db.sharding

import net.jkcode.jkmvc.db.Db
import java.sql.Connection

/**
 * 分布式的db
 *    使用 sharding-jdbc 来获得连接
 *    由于sharding-jdbc自身已实现主从, 因此不用手动再实现主从, 直接 slaveConn = masterConn
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-08-23 10:31 AM
 */
class ShardingDb(name:String /* 标识 */) : Db(name) {

    /**
     * 连接使用情况
     */
    protected var connUsed: Boolean = false

    /**
     * 主库连接
     */
    protected override val masterConn: Connection by lazy {
        val dataSource = ShardingDataSourceFactory.getDataSource(name)
        connUsed = true
        dataSource.connection
    }

    /**
     * 随机一个从库连接
     */
    protected override val slaveConn: Connection
        get() = masterConn

    /**
     * 关闭
     */
    public override fun close():Unit{
        super.close()

        if(connUsed)
            masterConn.close()
    }
}