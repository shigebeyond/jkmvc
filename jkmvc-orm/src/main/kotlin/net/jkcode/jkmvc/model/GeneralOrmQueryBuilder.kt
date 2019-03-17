package net.jkcode.jkmvc.model

import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkmvc.db.Row
import net.jkcode.jkmvc.orm.IOrmMeta
import net.jkcode.jkmvc.orm.OrmQueryBuilder

/**
 * 改写 OrmQueryBuilder, 查询 GeneralModel 后设置其 ormMeta
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 3:38 PM
 */
internal class GeneralOrmQueryBuilder(ormMeta: IOrmMeta /* orm元数据 */,
                                      convertingValue: Boolean = false /* 查询时是否智能转换字段值 */,
                                      convertingColumn: Boolean = false /* 查询时是否智能转换字段名 */,
                                      withSelect: Boolean = true /* with()联查时自动select关联表的字段 */
): OrmQueryBuilder(ormMeta, convertingValue, convertingColumn, withSelect){

    /**
     * 查找多个： select 语句
     *
     * @param params 参数
     * @param transform 转换函数
     * @return 列表
     */
    public override fun <T:Any> findAll(params: List<Any?>, db: IDb, transform: (Row) -> T): List<T>{
        val items = super.findAll(params, db, transform)
        if(items.isNotEmpty() && items.first() is GeneralModel){
            for(item in items)
                (item as GeneralModel).delaySetMeta(ormMeta)
        }
        return items
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 参数
     * @param transform 转换函数
     * @return 单个数据
     */
    public override fun <T:Any> find(params: List<Any?>, db: IDb, transform: (Row) -> T): T?{
        val item = super.find(params, db, transform)
        if(item != null && item is GeneralModel){
            item.delaySetMeta(ormMeta)
        }
        return item
    }
}