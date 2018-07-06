package com.jkmvc.db

import com.alibaba.druid.pool.DruidDataSource
import com.jkmvc.common.Config
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

/**
 * 数据源工厂
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
class DruidDataSourceFactory : IDataSourceFactory {

    /**
     * 缓存数据源
     */
    private val dataSources: ConcurrentHashMap<String, DruidDataSource> = ConcurrentHashMap<String, DruidDataSource>();

    /**
     * 获得数据源
     * @param name 数据源名
     * @return
     */
    public override fun getDataSource(name: String): DataSource {
        return dataSources.getOrPut(name){
            buildDruidDataSource(name)
        }
    }

    /**
     * 构建数据源
     * @param name 数据源名
     * @return
     */
    private fun buildDruidDataSource(name:String): DruidDataSource {
        val config: Config = Config.instance("database.$name", "yaml")
        val ds: DruidDataSource = DruidDataSource()

        // 基本属性 url、user、password
        ds.setUrl(config["url"])
        ds.setUsername(config["username"])
        ds.setPassword(config["password"])
        val driverClass: String = config["driverClass"]!!
        ds.setDriverClassName(driverClass)

        // 其他属性
        ds.setConnectionProperties(config["connectionProperties"])

        // 连接池大小
        ds.setInitialSize(config.getInt("initialSize", 10)!!) // 初始连接池大小
        ds.setMinIdle(config.getInt("minIdle", 10)!!) // 最小空闲连接数
        ds.setMaxActive(config.getInt("maxActive", 100)!!) // 最大活跃连接数，与并发线程数一致
        ds.setMaxWait(config.getLong("maxWait", DruidDataSource.DEFAULT_MAX_WAIT.toLong())!!) // 配置获取连接等待超时的时间
        ds.setTimeBetweenConnectErrorMillis(config.getLong("timeBetweenConnectErrorMillis", DruidDataSource.DEFAULT_TIME_BETWEEN_CONNECT_ERROR_MILLIS)!!) // 配置发生错误时多久重连
        ds.setTimeBetweenEvictionRunsMillis(config.getLong("timeBetweenEvictionRunsMillis", DruidDataSource.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS)!!) // 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
        ds.setMinEvictableIdleTimeMillis(config.getLong("minEvictableIdleTimeMillis", DruidDataSource.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS)!!) // 配置连接在池中最小生存的时间

        // 校验的sql
        ds.setValidationQuery(config.get("validationQuery", getValidatioQuery(driverClass)))
        ds.setValidationQueryTimeout(config.get("validationQueryTimeout", 30)!!)
        ds.setTestWhileIdle(config.getBoolean("testWhileIdle", true)!!)
        ds.setTestOnBorrow(config.getBoolean("testOnBorrow", true)!!)
        ds.setTestOnReturn(config.getBoolean("testOnReturn", true)!!)

        // 连接泄露检测
        ds.setRemoveAbandoned(config.getBoolean("removeAbandoned", false)!!) // 是否打开连接泄露自动检测
        ds.setRemoveAbandonedTimeoutMillis(config.getLong("removeAbandonedTimeoutMillis", 300 * 1000)!!) // 连接长时间没有使用，被认为发生泄露时长
        ds.setLogAbandoned(config.getBoolean("logAbandoned", false)!!)  // 发生泄露时是否需要输出 log，建议在开启连接泄露检测时开启，方便排错

        //只要maxPoolPreparedStatementPerConnectionSize>0,poolPreparedStatements就会被自动设定为true，参照druid的源码
        ds.setMaxPoolPreparedStatementPerConnectionSize(config.getInt("maxPoolPreparedStatementPerConnectionSize", -1)!!)

        // 查询超时(秒)
        if(config.containsKey("queryTimeOut"))
            ds.setQueryTimeout(config.getInt("queryTimeOut", 3)!!);

        // 配置监控统计拦截的filters
        val filters: String? = config["filters"]    // 监控统计："stat"    防SQL注入："wall"     组合使用： "stat,wall"
        if (!filters.isNullOrBlank())
            ds.setFilters(filters)

        return ds;
    }

    /**
     * 关闭所有数据源
     */
    public override fun closeAllDataSources():Unit{
        for((name, dataSource) in dataSources){
            dataSource.close()
        }
        dataSources.clear();
    }

    /**
     * 获得校验sql
     *
     * @param driverClass
     * @return
     */
    public fun getValidatioQuery(driverClass:String):String{
        val config = Config.instance("validation-query")
        return config[driverClass]!!
    }

}