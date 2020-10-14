package net.jkcode.jkmvc.orm.relation

import net.jkcode.jkmvc.orm.IOrm
import java.util.*

/**
 * 通过回调动态获得对象的关联关系
 *   回调可以是rpc
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2020-7-9 7:13 PM
 */
class CbRelationMeta<M: IOrm, K, R> (
        override val hasMany: Boolean, // 是否一对多
        override val pkGetter: (M)->K, // 主模型的主键的getter
        override val fkGetter: (R)->K, // 从对象的外键的getter
        override val relatedSupplier:(List<K>) -> List<R> // 批量获取关联对象的回调
) : ICbRelationMeta<M, K, R> {

    /**
     * 关系名
     */
    override lateinit var name: String

    /**
     * 查询关联对象
     *    自动根据关联关系，来构建查询条件
     *
     * @param item Orm对象
     * @return
     */
    override fun findRelated(item: M): R?{
        return findAllRelated(item).firstOrNull() 
    }  
    
    /**
     * 查询关联对象
     *    自动根据关联关系，来构建查询条件
     *
     * @param item Orm对象
     * @return
     */
    override fun findAllRelated(item: M): List<R>{
        val pk = pkGetter.invoke(item)
        return relatedSupplier.invoke(listOf(pk)) 
    }

    /**
     * 查询关联对象
     *    自动根据关联关系，来构建查询条件
     *
     * @param items Orm列表
     * @return
     */
    override fun findAllRelated(items: Collection<M>): List<R>{
        val pks = items.map(pkGetter)
        return relatedSupplier.invoke(pks)
    }


    /**
     * 设置关系的属性值
     *
     * @param items 本模型对象
     * @param relatedItems 关联模型对象
     */
    override fun setRelationProp(items: List<M>, relatedItems: List<R>) {
        if(items.isEmpty() || relatedItems.isEmpty())
            return

        // 设置关联属性 -- 双循环匹配主外键
        for (item in items) { // 遍历每个源对象，收集关联对象
            for (relatedItem in relatedItems) { // 遍历每个关联对象，进行匹配
                // 关系的匹配： 本表键=关联表键
                val pk = pkGetter.invoke(item) // 本表键
                val fk = fkGetter.invoke(relatedItem) // 关联表键
                if (pk != null && fk != null  && pk.equals(fk)) {
                    if(hasMany){ // hasMany关联对象是list
                        val myRelated = item.getOrPut(name){
                            LinkedList<Any?>()
                        } as LinkedList<Any?>
                        myRelated.add(relatedItem)
                    }else{ // 其他关联对象是单个对象
                        item[name] = relatedItem
                    }

                }
            }
        }

        // 清空列表
        (relatedItems as MutableList).clear()
    }
}