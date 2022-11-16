<?php
namespace php\jkmvc\orm;

/**
 * Class QueryBuilder
 * @package php\jkmvc\orm
 */
class QueryBuilder
{
    /**
     * 设置表名
     *
     * @param table 表名
     * @param alias 别名
     * @return
     */
    function table(string $table, string $alias = null): QueryBuilder {
    }

    /**
     * 设置表名
     *
     * @param table 表名
     * @param alias 别名
     * @return
     */
    function from(string $table, string $alias = null): QueryBuilder{
    }


    /**
     * 设置插入的列, insert时用
     *
     * @param column
     * @return
     */
    function insertColumns($colums): QueryBuilder{
    }

    /**
     * 设置插入的单行值, insert时用
     *    插入的值的数目必须登录插入的列的数目
     * @param row
     * @return
     */
    function value(array $row): QueryBuilder{
    }

    /**
     * 设置更新的单个值, update时用
     *
     * @param column
     * @param value
     * @return
     */
    function set(string $column, $value): QueryBuilder{
    }

    /**
     * 设置更新的多个值, update时用
     *
     * @param row
     * @return
     */
    function sets(array $row): QueryBuilder{
    }

    /**
     * 设置查询的字段, select时用
     *
     * @param columns 字段名数组，其元素类型是 String 或 Pair
     * @return
     */
    function select(array $columns): QueryBuilder{
    }

    /**
     * 设置查询结果是否去重唯一
     * @returnAction
     */
    function distinct(): QueryBuilder{
    }

    /**
     * 设置查询的字段, select时用
     *
     * @param columns 字段名数组，其元素类型是 String 或 Pair
     * @return
     */
    function selectDistinct($columns): QueryBuilder{
    }

    /**
     * Adds addition tables to "JOIN ...".
     *
     * @param   table  table name | DbExpr | subquery
     * @param   type   joinClause type (LEFT, RIGHT, INNER, etc)
     * @return
     */
    function join(string table, string $type = "INNER"): QueryBuilder{
    }

    /**
     * Adds "ON ..." conditions for the last created JOIN statement.
     *
     * @param   c1  column name or DbExpr
     * @param   op  logic operator
     * @param   c2  column name or DbExpr or value
     * @param   isCol whether is column name, or value
     * @return
     */
    function on(string $c1, string $op, $c2): QueryBuilder{
    }

    /**
     * 清空条件
     * @return
     */
    function clear(): QueryBuilder{
    }


    /**
     * 多个having条件
     * @param conditions
     * @return
     */
    function havings(array $conditions): QueryBuilder{
    }

    /**
     * Alias of andWhere()
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    function where(string $column, string $op, $value): QueryBuilder{
    }

    /**
     * Creates a new "WHERE BETWEEN" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   from   column value
     * @param   to   column value
     * @return
     */
    function whereBetween(string $column, $from, $to): QueryBuilder{
    }

    /**
     * Creates a new "OR WHERE BETWEEN" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   from   column value
     * @param   to   column value
     * @return
     */
    function orWhereBetween(string $column, $from, $to): QueryBuilder{
    }

    /**
     * Creates a new "WHERE LIKE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   value   column value
     * @return
     */
    function whereLike(string $column, string $value): QueryBuilder{
    }

    /**
     * Creates a new "OR WHERE LIKE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   value   column value
     * @return
     */
    function orWhereLike(string $column, string $value): QueryBuilder{
    }

    /**
     * Multiple Where
     *
     * @param   conditions
     * @return
     */
    function wheres(array $conditions): QueryBuilder{
    }

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    function andWhere(string $column, string $op, $value): QueryBuilder{
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    function orWhere(string $column, string $op, $value): QueryBuilder{
    }

    /**
     * Alias of andWhereCondition()
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    function whereCondition(string $condition, array $params = []): QueryBuilder{
    }

    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    function andWhereCondition(string $condition, array $params = []): QueryBuilder{
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    function orWhereCondition(string $condition, array $params = []): QueryBuilder{
    }

    /**
     * Alias of andWhereOpen()
     *
     * @return
     */
    function whereOpen(): QueryBuilder{
    }

    /**
     * Opens a new "AND WHERE (...)" grouping.
     *
     * @return
     */
    function andWhereOpen(): QueryBuilder{
    }

    /**
     * Opens a new "OR WHERE (...)" grouping.
     *
     * @return
     */
    function orWhereOpen(): QueryBuilder{
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    function whereClose(): QueryBuilder{
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    function andWhereClose(): QueryBuilder{
    }

    /**
     * Closes an open "WHERE (...)" grouping.
     *
     * @return
     */
    function orWhereClose(): QueryBuilder{
    }

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   column  column name
     * @return
     */
    function groupBy(string $column): QueryBuilder{
    }

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   columns  column name
     * @return
     */
    function groupBys($columns): QueryBuilder{
    }

    /**
     * Alias of andHaving()
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    function having(string $column, string $op, $value): QueryBuilder{
    }

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    function andHaving(string $column, string $op, $value): QueryBuilder{
    }

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   column  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    function orHaving(string $column, string $op, $value): QueryBuilder{
    }

    /**
     * Alias of andHavingCondition()
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    function havingCondition(string $condition, array $params = []): QueryBuilder{
    }

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    function andHavingCondition(string $condition, array $params = []): QueryBuilder{
    }

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   condition  condition expression
     * @param   params
     * @return
     */
    function orHavingCondition(string $condition, array $params = []): QueryBuilder{
    }

    /**
     * Alias of andHavingOpen()
     *
     * @return
     */
    function havingOpen(): QueryBuilder{
    }

    /**
     * Opens a new "AND HAVING (...)" grouping.
     *
     * @return
     */
    function andHavingOpen(): QueryBuilder{
    }

    /**
     * Opens a new "OR HAVING (...)" grouping.
     *
     * @return
     */
    function orHavingOpen(): QueryBuilder{
    }

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    function havingClose(): QueryBuilder{
    }

    /**
     * Closes an open "AND HAVING (...)" grouping.
     *
     * @return
     */
    function andHavingClose(): QueryBuilder{
    }

    /**
     * Closes an open "OR HAVING (...)" grouping.
     *
     * @return
     */
    function orHavingClose(): QueryBuilder{
    }

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   column     column name or DbExpr
     * @param   direction  direction of sorting
     * @return
     */
    function orderBy(string $column, string $direction = null): QueryBuilder{
    }

    /**
     * Multiple OrderBy
     *
     * @param orders
     * @return
     */
    function orderBys(array $orders): QueryBuilder{
    }


    /**
     * Return up to "LIMIT ..." results
     *
     * @param  limit
     * @param  offset
     * @return
     */
    function limit(int limit, int offset = 0): QueryBuilder{
    }

    /**
     * 设置查询加锁
     *
     * @return
     */
    function forUpdate(): QueryBuilder{
    }

    /****************************** 执行sql ********************************/
    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 参数
     * @return 一个数据
     */
    function findRow(array params = []): array{
    }

    /**
     * 查找全部： select ... 语句
     *
     * @param params 参数
     * @return 全部数据
     */
    function findRows(array params = []): array{
    }

    /**
     * 统计行数： count语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    function count(array $params = []):Int{
    }

    /**
     * 加总列值： sum语句
     *
     * @param column 列
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    function sum(string $column, array $params = []):Int{
    }

    /**
     * 插入：insert语句
     *
     *  @param generatedColumn 返回的自动生成的主键名
     *  @param params 参数
     *  @param db 数据库连接
     * @return 新增的id
     */
    function insert(string $generatedColumn, array $params = []): Long {
    }

    /**
     * 更新：update语句
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    function update(array $params = []): Boolean {
    }

    /**
     * 删除
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    function delete(array $params = []): Boolean {
    }

    /**
     * 自增
     *
     * @param params 参数
     * @param db 数据库连接
     * @return
     */
    function incr(string $column, int $step, array $params = []): Boolean{
    }

    /**
     * 批量插入
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param db 数据库连接
     * @return
     */
    function batchInsert(array $paramses): IntArray {
    }

    /**
     * 批量更新
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param db 数据库连接
     * @return
     */
    function batchUpdate(array $paramses): IntArray {
    }

    /**
     * 批量插入
     *
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param db 数据库连接
     * @return
     */
    function batchDelete(array $paramses) {
    }

    /****************************** OrmQueryBuilder实现 ********************************/
    /**
     * 联查单表
     *
     * @param name 关联关系名
     * @param columns 关联模型的字段列表
     * @return
     */
    function with(string $name, array $columns): QueryBuilder {
    }

    /**
     * 联查多表
     *
     * @param names 关联关系名的数组
     * @return
     */
    function withs($names): QueryBuilder {
    }

}