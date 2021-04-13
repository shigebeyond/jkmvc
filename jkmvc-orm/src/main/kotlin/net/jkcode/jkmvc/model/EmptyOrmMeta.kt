package net.jkcode.jkmvc.model

import net.jkcode.jkmvc.orm.IOrm
import net.jkcode.jkmvc.orm.OrmException
import net.jkcode.jkmvc.orm.OrmMeta
import net.jkcode.jkmvc.orm.OrmQueryBuilder
import kotlin.reflect.KClass

/**
 * 空的元数据
 *    实例化时还是需要的元数据 EmptyOrmMeta, 但他不是真正的元数据，只为了在 KClass<T>.modelRowTransformer　中使用
 *    真正的元数据设置是在 GeneralOrmQueryBuilder.findRow()/findRows() 调用 GeneralModel.delaySetMeta()
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 3:38 PM
 */
open class EmptyOrmMeta(model: KClass<out IOrm> /* 模型类 */): OrmMeta(model, "?", "?", "?", checkingTablePrimaryKey = false){

                        /**
     * 禁用 queryBuilder()
     *
     * @param convertingValue 查询时是否智能转换字段值
     * @param convertingColumn 查询时是否智能转换字段名
     * @param withSelect with()联查时自动select关联表的字段
     * @param reused 是否复用的
     * @return
     */
    public override fun queryBuilder(convertingValue: Boolean, convertingColumn: Boolean, withSelect: Boolean, reused: Boolean): OrmQueryBuilder {
        throw OrmException("class [EmptyOrmMeta] does not support method [queryBuilder()]")
    }
}
