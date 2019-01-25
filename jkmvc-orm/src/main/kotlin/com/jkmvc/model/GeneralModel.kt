package com.jkmvc.model

import com.jkmvc.db.MutableRow
import com.jkmvc.db.Row
import com.jkmvc.orm.*
import java.util.*

/**
 * 通用模型
 * 1 动态元数据
 *   不用在声明model类时就指定元数据, 而是递延到model类实例化时,才动态的指定元数据
 *
 * 2 两个构造函数, 按需选用
 *   主构造函数是直接指定IOrmMeta对象, 一般用于指定复杂的元数据
 *   辅构造函数是通过指定表名+主键字段名, 来快速构建简单的元数据
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 3:38 PM
 */
class GeneralModel(myOrmMeta: IOrmMeta /* 自定义元数据 */) : Orm(emptyArray()) {

    public constructor(table: String /* 表名 */, primaryKey:String = "id" /* 主键 */):this(OrmMeta(GeneralModel::class, "`$table`'s general model", table, primaryKey))

    // 仅在内部使用 (如 KClass<T>.rowTransformer), 不暴露给外部
    internal constructor():this(EmptyOrmMeta)

    /**
     * 改写ormMeta -- 增删改查时需要的元数据
     */
    public override var ormMeta: IOrmMeta = GeneralOrmMeta(myOrmMeta)

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
     * @return
     */
    public override fun setOriginal(orgn: Row): IOrm {
        tempOriginal.putAll(orgn)
        return this
    }

    /**
     * 延迟设置元数据 =>　递延设置原始字段值
     * @param ormMeta
     * @return
     */
    public fun delaySetMeta(ormMeta: IOrmMeta): IOrm {
        this.ormMeta = ormMeta
        // 设置原始字段值
        super.setOriginal(tempOriginal)
        tempOriginal.clear()
        return this
    }
}

