package net.jkcode.jkmvc.db

import java.io.Closeable
import javax.sql.DataSource

/**
 * 可关闭的数据源
 * @author shijianhang
 * @date 2020-3-8 下午8:02:47
 */
class ClosableDataSource(protected val ds: DataSource): Closeable, DataSource by ds {

    /**
     * 关闭数据源
     *   调用被代理的数据源自身实现的close()方法
     */
    public override fun close(){
        val method = ds.javaClass.getMethod("close")
        method.invoke(ds)
    }

    override fun equals(other: Any?): Boolean {
        return other is DataSource && ds == other
    }
}