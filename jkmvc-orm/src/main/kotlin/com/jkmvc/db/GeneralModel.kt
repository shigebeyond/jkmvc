package com.jkmvc.db

import com.jkmvc.orm.IOrmMeta
import com.jkmvc.orm.Orm
import com.jkmvc.orm.OrmMeta

/**
 * 通用模型
 * 1 动态元数据
 *   不用在声明model类时就指定元数据, 而是递延到model类实例化时,才动态的指定元数据
 *
 * 2 两个构造函数, 按需选用
 *   主构造函数是直接指定IOrmMeta对象工厂的lambda, 一般用于指定复杂的元数据
 *   辅构造函数是通过指定表名+主键字段名, 来快速构建简单的元数据
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 3:38 PM
 */
class GeneralModel(public override val ormMeta: IOrmMeta /* 元数据构建器 */) : Orm(emptyArray()) {

    public constructor(table: String /* 表名 */, primaryKey:String = "id" /* 主键 */):this(OrmMeta(GeneralModel::class, "`$table`'s general model", table, primaryKey, needInstanceInit = false /* use Unsafe to instantiate */))

    /**
     * 实例化时需要的元数据，在 KClass<T>.rowTranformer　中使用
     */
    companion object m: OrmMeta(GeneralModel::class, "?", "?", "?", needInstanceInit = false /* use Unsafe to instantiate */)
}