package net.jkcode.jkmvc.model

import net.jkcode.jkmvc.db.MutableRow
import net.jkcode.jkmvc.db.Row
import net.jkcode.jkmvc.orm.*
import java.util.*

/**
 * 通用模型
 * 1 动态元数据
 *   不用在声明model类时就指定元数据, 而是递延到model类实例化时,才动态的指定元数据
 *
 * 2 两个构造函数, 按需选用
 *   主构造函数是直接指定元数据, 可以是 1 OrmMeta 普通元数据 2 GeneralOrmMeta 通用元数据 3 EmptyOrmMeta 空的元数据
 *   辅构造函数是通过指定表名+主键字段名, 来快速构建 GeneralOrmMeta 通用元数据
 *
 * 3 如果元数据是 GeneralOrmMeta 通用元数据
 * 3.1 GeneralOrmMeta 通用元数据
 *      改写 queryBuilder(), 返回 GeneralOrmQueryBuilder
 * 3.2 GeneralOrmQueryBuilder
 *     改写 OrmQueryBuilder, 在查询并创建 GeneralModel 后才能由 GeneralOrmQueryBuilder 设置其 ormMeta, 即调用 GeneralModel.delaySetMeta(ormMeta)
 *     因为创建过程在 KClass<T>.rowTransformer(), 无 ormMeta 参数, 只能使用默认构造函数, 给默认参数 EmptyOrmMeta
 *
 * 4 如果元数据是 OrmMeta 普通元数据
 *   跟普通的model一样, 只是可能使用 GeneralModel 来做代理, 如 `class MessageModel: IOrm by GeneralModel(m)`
 *   此时 GeneralModel 是必有
 *
 * 5 如果元数据是 EmptyOrmMeta 空的元数据
 *   只在  KClass<T>.rowTransformer() 中使用默认构造函数实例化时才出现, 这种情况需要延迟 1 设置 ormMeta 2 调用 super.setOriginal(orgn)
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 3:38 PM
 */
class GeneralModel(myOrmMeta: IOrmMeta /* 自定义元数据 */) : Orm(emptyArray()) {

    public constructor(table: String /* 表名 */, primaryKey:String = "id" /* 主键 */):this(GeneralOrmMeta(GeneralModel::class, "`$table`'s general model", table, primaryKey))

    // 仅在内部使用 (如 KClass<T>.rowTransformer), 不暴露给外部
    internal constructor():this(EmptyOrmMeta)

    /**
     * 改写ormMeta -- 增删改查时需要的元数据
     */
    public override var ormMeta: IOrmMeta = myOrmMeta

    /**
     * 伴随对象 -- 实例化时需要的元数据，在 KClass<T>.rowTransformer　中使用
     */
    companion object EmptyOrmMeta: OrmMeta(GeneralModel::class, "?", "?", "?"){

        /**
         * 禁用 queryBuilder()
         *
         * @param convertingValue 查询时是否智能转换字段值
         * @param convertingColumn 查询时是否智能转换字段名
         * @param withSelect with()联查时自动select关联表的字段
         * @return
         */
        public override fun queryBuilder(convertingValue: Boolean, convertingColumn: Boolean, withSelect: Boolean): OrmQueryBuilder {
            throw OrmException("class [EmptyOrmMeta] does not support method [queryBuilder()]")
        }
    }

    /**
     * 临时存储的原始字段值
     */
    protected val tempOriginal: MutableRow by lazy{
        HashMap<String, Any?>()
    }

    /**
     * 改写 setOriginal(), 将原始字段值临时存储 tempOriginal
     * @param data
     */
    public override fun setOriginal(orgn: Row): Unit {
        if(ormMeta == EmptyOrmMeta)
            tempOriginal.putAll(orgn)
        else
            super.setOriginal(orgn)
    }

    /**
     * 延迟设置元数据 =>　递延设置原始字段值
     * @param ormMeta
     */
    public fun delaySetMeta(ormMeta: IOrmMeta) {
        this.ormMeta = ormMeta
        // 设置原始字段值
        super.setOriginal(tempOriginal)
        tempOriginal.clear()
    }
}

