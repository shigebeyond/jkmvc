package net.jkcode.jkmvc.query

import net.jkcode.jkmvc.db.IDbIdentifierQuoter

/**
 * Db表达式
 * 1 带别名
 * 2 控制是否转义
 *   用来在 DbQueryBuilder 的select/insert/update语句中，添加不转义的字段值，表示要保存的字段值是一个sql表达式，如 now() / column1 + 1, 如
 *   <code>
 *     // SELECT  COUNT(`id`) AS `total_posts`, `username` FROM `posts`
 *     DbQueryBuilder().select("username", DbExpr("COUNT(`id`)", "total_posts", false)).from("posts")
 *     // UPDATE `user` SET `login_count` = `login_count` + 1 WHERE `id` = 45
 *     DbQueryBuilder().table("user").set("login_count", DbExpr("login_count + 1", false)).where("id", "=", 45).update();
 *   </code>
 * 3 CharSequence接口
 *   为了适配 DbQueryBuilder 中的查询方法的查询参数类型, 如 select() / where()
 *   否则要重载很多方法来接收 DbExpr 参数
 *
 * @author shijianhang
 * @create 2017-11-19 下午1:47
 **/
data class DbExpr(public val exp:CharSequence, // 表达式, 可以是 String | DbQueryBuilder
                  public val alias:String?, // 别名
                  public val expQuoting:Boolean = (exp !is IDbQueryBuilder) // 是否转义exp, 只要不是子查询/函数, 默认都转
) : CharSequence by exp {

    companion object {

        /**
         * 空表/空字段
         */
        public val empty = DbExpr("", null)

        /**
         * 问号, 用于表示query builder的动态参数
         */
        public val question = DbExpr("?", false)
    }

    public constructor(exp:CharSequence, quoting:Boolean): this(exp, null, quoting)

    init {
        //表达式只能是 String | DbQueryBuilder
        if(exp !is String && exp !is IDbQueryBuilder){
            throw IllegalArgumentException("表达式只能是 String 或 DbQueryBuilder 类型, 但现在是 ${exp.javaClass} 类型: $exp")
        }
        // 检查子查询与转义不能并存
        if(exp is IDbQueryBuilder && expQuoting)
            throw IllegalArgumentException("如果exp是子查询, 就不能指定expQuoting=true")
    }

    /**
     * 转字符串
     * @return
     */
    public override fun toString(): String {
        if(alias == null)
            return exp.toString();

        return "$exp $alias"
    }

    /**
     * 转义
     *   mysql为`xxx`
     *   oracle为"xxx"
     *   sql server为"xxx" [xxx]
     *
     * @param identifierQuoteString 转义符
     * @param delimiter 连接符, 连接表达式+别名
     * @return
     */
    public fun quoteIdentifier(quoter: IDbIdentifierQuoter, delimiter:String = " "): String{
        // 转义别名
        val alias2 = if(alias == null)
                        ""
                    else
                        quoter.quoteIdentifier(alias)
        // 转义表达式
        return if(expQuoting) // 转
                    "${quoter.quoteIdentifier(exp.toString())}$delimiter$alias2"
                else // 不转
                    "$exp$delimiter$alias2"
    }
}