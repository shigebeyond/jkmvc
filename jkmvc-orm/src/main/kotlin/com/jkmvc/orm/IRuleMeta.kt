package com.jkmvc.orm

import com.jkmvc.db.Db
import com.jkmvc.db.IDbQueryBuilder
import kotlin.reflect.KClass

/**
 * 字段校验规则的元数据
 *    标签 + 校验规则
 * @author shijianhang
 * @date 2016-10-10
 */
interface IRuleMeta {

    /**
     * 字段标签（中文名）
     */
    val label:String

    /**
     * 字段的校验规则
     */
    val rule: String?
}
