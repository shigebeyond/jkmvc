package com.jkmvc.db

/**
 * Db表达式，用来在 DbQueryBuilder 的insert/update语句中，添加不转义的字段值，表示要保存的字段值是一个sql表达式，如 now() / column1 + 1
 *   例如
 *     // UPDATE `user` SET `login_count` = `login_count` + 1 WHERE `id` = 45
 *     DbQueryBuilder().table("user").set("login_count", DbExpression("login_count + 1")).where("id", "=", 45).update();
 *
 * @author shijianhang
 * @create 2017-11-19 下午1:47
 **/
class DbExpression(protected val exp:String /* sql表达式 */) : CharSequence by exp {

    /**
     * 转字符串
     * @return
     */
    public override fun toString(): String {
        return exp
    }
}