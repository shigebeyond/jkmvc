package net.jkcode.jkmvc.model

import net.jkcode.jkmvc.db.DbResultRow
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkmvc.orm.IOrmMeta
import net.jkcode.jkmvc.orm.OrmMeta
import net.jkcode.jkmvc.orm.OrmQueryBuilder

/**
 * 通用模型的查询构建器
 *    改写 OrmQueryBuilder, 在查询并创建 GeneralModel 后才能由 GeneralOrmQueryBuilder 设置其 ormMeta, 即调用 GeneralModel.delaySetMeta(ormMeta)
 *    因为创建过程在 KClass<T>.modelRowTransformer(), 无 ormMeta 参数, 只能使用默认构造函数, 给默认参数 EmptyOrmMeta
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 3:38 PM
 */
class GeneralOrmQueryBuilder(ormMeta: OrmMeta, // orm元数据
                             convertingValue: Boolean = false, // 查询时是否智能转换字段值
                             convertingColumn: Boolean = false, // 查询时是否智能转换字段名
                             withSelect: Boolean = true // with()联查时自动select关联表的字段
): OrmQueryBuilder(ormMeta, convertingValue, convertingColumn, withSelect){

    /**
     * 包装行转换函数
     * @param transform 行转换函数, 在 findRows()/findRow() 中使用
     * @return
     */
    protected fun <T> wrapRowTransform(transform: (DbResultRow) -> T): (DbResultRow) -> T {
        return { row ->
            val item = transform.invoke(row)
            if(item != null && item is GeneralModel)
                item.delaySetMeta(ormMeta)
            item
        }
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 参数
     * @param transform 行转换函数
     * @return 单个数据
     */
    public override fun <T:Any> findRow(params: List<*>, db: IDb, transform: (DbResultRow) -> T): T?{
        return super.findRow(params, db, wrapRowTransform(transform) /* 包装行转换函数 */)
    }

    /**
     * 查找多个： select 语句
     *
     * @param params 参数
     * @param transform 行转换函数
     * @return 列表
     */
    public override fun <T:Any> findRows(params: List<*>, db: IDb, transform: (DbResultRow) -> T): List<T>{
        return super.findRows(params, db, wrapRowTransform(transform) /* 包装行转换函数 */)
    }
}