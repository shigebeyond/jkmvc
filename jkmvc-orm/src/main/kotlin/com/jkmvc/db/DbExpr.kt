package com.jkmvc.db

/**
 * Db表达式
 * 1 带别名
 * 2 控制是否转义
 *   用来在 DbQueryBuilder 的select/insert/update语句中，添加不转义的字段值，表示要保存的字段值是一个sql表达式，如 now() / column1 + 1, 如
 *   <code>
 *     // UPDATE `user` SET `login_count` = `login_count` + 1 WHERE `id` = 45
 *     DbQueryBuilder().table("user").set("login_count", DbExpr("login_count + 1", false)).where("id", "=", 45).update();
 *   </code>
 *
 * @author shijianhang
 * @create 2017-11-19 下午1:47
 **/
data class DbExpr(public val exp:CharSequence /* 原名, 可以是 String | DbQueryBuilder */,
                  public val alias:String?, /* 别名 */
                  public val quoting:Boolean = true /* 是否转义 */
) : CharSequence by "" {

    public constructor(exp:CharSequence, quoting:Boolean): this(exp, null, quoting)

    /**
     * 转字符串
     * @return
     */
    public override fun toString(): String {
        if(alias == null)
            return exp.toString();

        return "$exp $alias"
    }
}