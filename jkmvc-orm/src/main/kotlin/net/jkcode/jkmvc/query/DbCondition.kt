package net.jkcode.jkmvc.query

/**
 * Db条件, 带参数, 专门用于 DbQueryBuilderDecoration.andWhereCondition()/orWhereCondition(), 用来表示不转义不拼接的条件表达式, 生成sql时原样输出
 *
 *
 * @author shijianhang
 * @create 2017-11-19 下午1:47
 **/
data class DbCondition(public val exp:String, // 条件表达式
                       public val params: List<*> = emptyList<Any>() // 参数
) : CharSequence by exp {

    /**
     * 转字符串
     * @return
     */
    public override fun toString(): String {
        return exp
    }

}