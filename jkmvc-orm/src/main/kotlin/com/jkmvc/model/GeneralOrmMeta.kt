package net.jkcode.jkmvc.model

import net.jkcode.jkmvc.orm.IOrmMeta
import net.jkcode.jkmvc.orm.OrmQueryBuilder

/**
 * 代理并改写元数据
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 3:38 PM
 */
internal open class GeneralOrmMeta(val source: IOrmMeta): IOrmMeta by source{

    /**
     * 改写 queryBuilder(), 返回 GeneralOrmQueryBuilder
     *
     * @param convertingValue 查询时是否智能转换字段值
     * @param convertingColumn 查询时是否智能转换字段名
     * @param withSelect with()联查时自动select关联表的字段
     * @return
     */
    public override fun queryBuilder(convertingValue: Boolean, convertingColumn: Boolean, withSelect: Boolean): OrmQueryBuilder {
        return GeneralOrmQueryBuilder(this, convertingValue, convertingColumn, withSelect);
    }
}