package net.jkcode.jkmvc.db

import net.jkcode.jkmvc.common.iteratorArrayOrCollection
import net.jkcode.jkmvc.common.joinToString

/**
 * Db值转义器
 *
 * @ClassName: IDbValueQuoter
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2018-11-21 7:28 PM
 */
interface IDbValueQuoter {

    /**
     * 转义值
     *
     * @param value 字段值, 可以是值数组
     * @return
     */
    fun quote(value:Any?):String {
        // 1 多值
        val itr = value?.iteratorArrayOrCollection()
        if(itr != null){
            return itr.joinToString(", ", "(", ")") {
                quote(it)
            }
        }

        // 2 单值
        return quoteSingleValue(value)
    }

    /**
     * 转义单个值
     *
     * @param value 字段值
     * @return
     */
    fun quoteSingleValue(value: Any?): String
}