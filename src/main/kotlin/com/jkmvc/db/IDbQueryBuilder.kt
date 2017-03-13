package com.jkmvc.db

import java.sql.Connection

/**
 * sql构建器
 *   依次继承 DbQueryBuilderAction 处理动作子句 + DbQueryBuilderDecoration 处理修饰子句
 *  提供select/where等类sql的方法, 但是调用方法时, 不直接拼接sql, 而是在compile()时才延迟拼接sql, 因为调用方法时元素可以无序, 但生成sql时元素必须有序
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-13
 *
 */
interface IDbQueryBuilder:IDbQueryBuilderAction, IDbQueryBuilderDecoration
{
    /**
     * 编译sql
     *
     * @param string action sql动作：select/insert/update/delete
     * @return Pair(sql, 参数)
     */
    public fun compile(action:String): Pair<String, List<Any?>>;

    /**
     * 查找多个： select 语句
     *
     * @return List
     */
    public fun findAll():List<Any>;

    /**
     * 查找一个： select ... limit 1语句
     *
     * @return object
     */
    public fun find():Any?;

    /**
     * 统计行数： count语句
     * @return int
     */
    public fun count():Int;

    /**
     * 插入：insert语句
     * @return int 新增的id
     */
    public fun insert():Int;

    /**
     *	更新：update语句
     *	@return	bool
     */
    public fun update():Boolean;

    /**
     *	删除
     *	@return	bool
     */
    public fun delete():Boolean;
}