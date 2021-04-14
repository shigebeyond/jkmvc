package net.jkcode.jkmvc.db.single

import com.alibaba.druid.pool.DruidDataSource
import net.jkcode.jkmvc.db.IDataSourceFactory
import net.jkcode.jkutil.common.Config
import javax.sql.DataSource

abstract class BaseDataSourceFactory: IDataSourceFactory(){

    /**
     * 构建数据源
     * @param name 数据源名
     * @return
     */
    override fun buildDataSource(name: CharSequence): DataSource {
        val config: Config = Config.instance("dataSources.$name", "yaml")
        return buildDataSource(config)
    }

    /**
     * 构建数据源
     * @param config 数据源和配置
     * @return
     */
    public abstract fun buildDataSource(config: Config): DataSource

    /**
     * 获得校验sql
     *
     * @param driverClass
     * @return
     */
    public fun getValidationQuery(driverClass:String):String{
        val config = Config.instance("validation-query")
        return config[driverClass]!!
    }

}