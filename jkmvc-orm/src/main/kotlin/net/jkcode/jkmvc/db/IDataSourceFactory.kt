package net.jkcode.jkmvc.db

import net.jkcode.jkutil.scope.ClosingOnShutdown
import net.jkcode.jkutil.common.getOrPutOnce
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

/**
 * 数据源工厂
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
abstract class IDataSourceFactory : ClosingOnShutdown() {

    /**
     * 缓存数据源
     */
    private val dataSources: ConcurrentHashMap<String, DataSource> = ConcurrentHashMap();

    /**
     * 获得数据源
     *    跨线程跨请求, 全局共有的数据源
     * @param name 数据源名
     * @return
     */
    public fun getDataSource(name: String): DataSource {
        return dataSources.getOrPutOnce(name){
            buildDataSource(name)
        }
    }

    /**
     * 构建数据源
     * @param name 数据源名
     * @return
     */
    protected abstract fun buildDataSource(name:String): DataSource

    /**
     * 关闭所有数据源
     */
    public override fun close(){
        for((name, dataSource) in dataSources){
            (dataSource as AutoCloseable).close()
        }
        dataSources.clear();
    }

}