package com.jkmvc.db

import com.alibaba.druid.pool.DruidDataSource

/**
 * 数据源工厂
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
interface IDataSourceFactory {

    /**
     * 获得数据源
     * @param name 数据源名
     * @return
     */
    fun getDataSource(name: String = "default"): DruidDataSource;

    /**
     * 关闭所有数据源
     */
    fun closeAllDataSources():Unit;

}