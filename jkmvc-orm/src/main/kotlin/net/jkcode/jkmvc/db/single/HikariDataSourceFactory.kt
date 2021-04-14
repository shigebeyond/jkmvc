package net.jkcode.jkmvc.db.single

import com.zaxxer.hikari.HikariDataSource
import net.jkcode.jkutil.common.Config
import javax.sql.DataSource

/**
 * Hikari数据源工厂
 *   参考
 *   https://www.jianshu.com/p/5b2646ed7bc7
 *   https://www.cnblogs.com/wangcp-2014/p/12155991.html
 *
 * @author shijianhang
 * @date 2021-3-8 下午8:02:47
 */
object HikariDataSourceFactory: BaseDataSourceFactory() {

    /**
     * 构建数据源
     * @param config 数据源和配置
     * @return
     */
    public override fun buildDataSource(config: Config): DataSource {
        val ds = HikariDataSource()

        // 基本属性 url、user、password
        ds.setJdbcUrl(config["url"])
        ds.setUsername(config["username"])
        ds.setPassword(config["password"])
        val driverClass: String = config["driverClassName"]!!
        ds.setDriverClassName(driverClass)

        ds.setConnectionTestQuery(config.get("validationQuery", getValidationQuery(driverClass)))
        ds.setConnectionTimeout(config.getLong("connectionTimeOut", 30000)!!) // 等待连接的超时(毫秒), 缺省:30秒
        ds.setIdleTimeout(config.getLong("idleTimeout", 600000)!!) // 连接空闲的最大时长(毫秒), 缺省:10分钟
        ds.setMaxLifetime(config.getLong("maxLifetime", 1800000)!! ) // 连接的生命时长(毫秒)，超时且没被使用则被释放(retired)，缺省:30分钟
        ds.setMaximumPoolSize(config.getInt("maxPoolSize", 10)!!) // 最大连接数, 默认10, 推荐的公式：((core_count * 2) + effective_spindle_count)
        ds.setMinimumIdle(config.getInt("minIdle", 10)!!) // 最小空闲数 默认 10

        return ds;
    }

}