package net.jkcode.jkmvc.db.single

import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.randomInt
import net.jkcode.jkmvc.db.Db
import java.sql.Connection

/**
 * 单机的db
 *    使用 druid 来获得连接
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-08-23 10:31 AM
 */
class SingleDb(name:String /* 标识 */) : Db(name) {

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
     * 主库连接
     */
    protected override val masterConn: Connection by lazy{
        //获得主库数据源
        val dataSource = DruidDataSourceFactory.getDataSource("$name.master");
        // 记录用到主库
        connUsed = connUsed or 1
        // 新建连接
        dataSource.connection
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
            val dataSource = DruidDataSourceFactory.getDataSource("$name.slaves.$i");
            // 记录用到从库
            connUsed = connUsed or 2
            // 新建连接
            dataSource.connection
        }
    }

    /**
     * 关闭
     */
    public override fun close():Unit{
        super.close()

        // 关闭主库连接
        if(connUsed and 1 > 0)
            masterConn.close()
        // 关闭从库连接
        if(connUsed and 2 > 0)
            if(connUsed and 1 == 0 || masterConn != slaveConn) // 检查从库 != 主库, 防止重复关闭
                slaveConn.close()
    }
}