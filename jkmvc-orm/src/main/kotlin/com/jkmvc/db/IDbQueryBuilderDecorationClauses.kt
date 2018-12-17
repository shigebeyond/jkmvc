package com.jkmvc.db

/**
 * sql修饰子句的模拟构建
 *     每个修饰子句(如where xxx and yyy/group by xxx, yyy)包含多个子表达式(如where可以有多个条件子表达式, 如name="shi", age=1), 每个子表达式有多个元素组成(如name/=/"shi")
 *     每个元素有对应的处理函数
 *     T是子表达式的类型
 *
 * @author shijianhang
 * @date 2016-10-13
 */
interface IDbQueryBuilderDecorationClauses<T> {

    /**
     * 编译多个子表达式
     *
     * @param db 数据库连接
     * @param sql 保存编译sql
     */
    fun compile(db: IDb, sql:StringBuilder):Unit;

    /**
     * 添加一个子表达式+连接符
     *
     * @param subexp 子表达式
     * @param delimiter 当前子表达式的连接符
     * @return
     */
    fun addSubexp(subexp:Array<Any?>, delimiter:String = ", "):IDbQueryBuilderDecorationClauses<T>;

    /**
     * 编译一个子表达式
     *
     * @param subexp 子表达式
     * @param j 索引
     * @param db 数据库连接
     * @param sql 保存编译的sql
     */
    fun compileSubexp(subexp:T, j:Int, db: IDb, sql:StringBuilder):Unit;

    /**
     * 开启一个分组
     *
     * @param delimiter 连接符
     * @return
     */
    fun open(delimiter:String):IDbQueryBuilderDecorationClauses<T>;

    /**
     * 结束一个分组
     *
     * @return
     */
    fun close():IDbQueryBuilderDecorationClauses<T>;

    /**
     * 清空
     * @return
     */
    fun clear():IDbQueryBuilderDecorationClauses<T>;
}