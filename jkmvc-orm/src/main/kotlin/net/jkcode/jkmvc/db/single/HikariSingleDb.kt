package net.jkcode.jkmvc.db.single

import net.jkcode.jkmvc.db.ClosableDataSource

/**
 * 单机的db
 *    使用 Hikari 来获得连接
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-08-23 10:31 AM
 */
class HikariSingleDb(name:String /* 标识 */) : BaseSingleDb(name) {

    /**
     * 获得数据源
     * @param name 数据源名
     * @return
     */
    protected override fun getDataSource(name: String): ClosableDataSource {
        return HikariDataSourceFactory.getDataSource(name);
    }

}