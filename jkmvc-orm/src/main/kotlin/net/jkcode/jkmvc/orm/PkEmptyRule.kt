package net.jkcode.jkmvc.orm

/**
 * 检查主键为空的规则
 *
 * @author shijianhang
 * @date 2020-2-1 12:00 PM
 */
class PkEmptyRule(public var rule: Int = 0) {

    companion object{

        // 允许数字0
        public val ALLOW_NUMBER_0: Int = 0

        // 允许空字符串
        public val ALLOW_STRING_EMPTY: Int = 1
        
        public val default = PkEmptyRule(0)
    }

    /**
     * 允许位为true
     * @param bit
     */
    public fun allow(bit: Int) {
        rule = rule or (1 shl bit)
    }

    /**
     * 是否允许位为true
     * @param bit
     * @return
     */
    public fun isAllow(bit: Int): Boolean {
        return rule and (1 shl bit) > 0
    }

    /**
     * 检查主键值是否为空
     * @param pk 主键值
     * @return
     */
    public fun isEmpty(pk: Any?): Boolean {
        return when(pk){
            null -> true
            is Int -> !isAllow(ALLOW_NUMBER_0) && pk == 0 // 数字0
            is String -> !isAllow(ALLOW_STRING_EMPTY) && pk.isBlank() // 空字符串
            is DbKey<*> -> pk.columns.any(::isEmpty) // 复合主键中的任一字段值不能为空
            else -> false
        }
    }


}