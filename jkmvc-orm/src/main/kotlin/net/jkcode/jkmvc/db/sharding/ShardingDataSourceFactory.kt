package net.jkcode.jkmvc.db.sharding

import net.jkcode.jkmvc.db.IDataSourceFactory
import java.io.File
import javax.sql.DataSource

/**
 * 数据源工厂
 *   使用 sharding-jdbc
 *
 * @author shijianhang
 * @date 2019-8-23 8:02:47
 */
object ShardingDataSourceFactory : IDataSourceFactory() {

    /**
     * 构建数据源
     * @param name 数据源名, 对应配置文件 dataSource-$name.yaml
     * @return
     */
    override fun buildDataSource(name:String): DataSource {
        val configFile = Thread.currentThread().contextClassLoader.getResource("dataSource-$name.yaml").getFile()
        return io.shardingjdbc.core.api.ShardingDataSourceFactory.createDataSource(File(
                configFile))
    }

}