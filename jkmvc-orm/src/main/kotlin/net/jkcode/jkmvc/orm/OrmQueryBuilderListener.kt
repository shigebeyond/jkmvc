package net.jkcode.jkmvc.orm

/**
 * OrmQueryBuilder的事件监听器
 *   例子
 *   1. 查询前置处理: 对某个模型类的 queryBuilder 做全局的配置, 如添加全局的where条件
 *   2. 更新后置处理: 关联对象的增删改, 需要通知主对象, 以便刷新主对象的缓存
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2020-04-22 2:49 PM
 */
abstract class OrmQueryBuilderListener {

    /**
     * 查询前置处理
     */
    open fun beforeFind(query: OrmQueryBuilder){}

    /**
     * 查询后置处理
     */
    open fun afterFind(query: OrmQueryBuilder){}

    /**
     * 更新前置处理
     */
    open fun beforeExecute(query: OrmQueryBuilder){}

    /**
     * 更新后置处理
     */
    open fun afterExecute(query: OrmQueryBuilder){}

}