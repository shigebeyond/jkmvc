package net.jkcode.jkmvc.orm.relation

import net.jkcode.jkmvc.orm.IOrm

/**
 * 通过回调动态获得对象的关联关系
 *   回调可以是rpc
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2020-7-9 7:13 PM
 */
interface ICbRelation<M : IOrm, K, R> {

    /**
     * 关系名
     */
    val name: String

    /**
     * 是否一对多
     */
    val one2one: Boolean

    /**
     * 主模型的主键的getter
     */
    val pkGetter: (M)->K

    /**
     * 从对象的外键的getter
     */
    val fkGetter: (R)->K

    /**
     * 批量获取关联对象的回调
     */
    val relatedSupplier:(List<K>) -> List<R>
    
    /**
     * 查询关联对象
     *    自动根据关联关系，来构建查询条件
     *
     * @param item Orm对象
     * @return
     */
    fun findRelated(item: M): R?

    /**
     * 查询关联对象
     *    自动根据关联关系，来构建查询条件
     *
     * @param orm Orm对象或列表
     * @return
     */
    fun findAllRelated(orm: Any): List<R>{
        return when(orm){
            is IOrm -> findAllRelated(orm)
            is Collection<*> -> findAllRelated(orm as Collection<M>)
            else -> throw IllegalArgumentException("对relation.findAllRelated(参数)方法，其参数必须是Orm对象或Orm列表")
        }
    }

    /**
     * 查询关联对象
     *    自动根据关联关系，来构建查询条件
     *
     * @param item Orm对象
     * @return
     */
    fun findAllRelated(item: M): List<R>

    /**
     * 查询关联对象
     *    自动根据关联关系，来构建查询条件
     *
     * @param items Orm列表
     * @return
     */
    fun findAllRelated(items: Collection<M>): List<R>

    /**
     * 批量设置关系的属性值
     *
     * @param items 本模型对象
     * @param relatedItems 关联模型对象
     */
    fun batchSetRelationProp(items: List<M>, relatedItems: List<R>)
}