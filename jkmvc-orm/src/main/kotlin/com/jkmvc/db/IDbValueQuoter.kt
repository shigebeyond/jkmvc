package com.jkmvc.db

/**
 * Db值转义器
 *
 * @ClassName: IDbQuoter
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-11-21 7:28 PM
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
        if(value is Array<*>){
            return value.joinToString(", ", "(", ")") {
                quote(it)
            }
        }
        if(value is IntArray){
            return value.joinToString(", ", "(", ")") {
                quote(it)
            }
        }
        if(value is ShortArray){
            return value.joinToString(", ", "(", ")") {
                quote(it)
            }
        }
        if(value is LongArray){
            return value.joinToString(", ", "(", ")") {
                quote(it)
            }
        }
        if(value is FloatArray){
            return value.joinToString(", ", "(", ")") {
                quote(it)
            }
        }
        if(value is DoubleArray){
            return value.joinToString(", ", "(", ")") {
                quote(it)
            }
        }
        if(value is BooleanArray){
            return value.joinToString(", ", "(", ")") {
                quote(it)
            }
        }
        if(value is Collection<*>){
            return value.joinToString(", ", "(", ")") {
                quote(it)
            }
        }

        // 2 单值
        return quoteSingleValue(value)
    }

    /**
     * 转义单个值
     *
     * @param value 字段值, 可以是值数组
     * @return
     */
    fun quoteSingleValue(value: Any?): String
}