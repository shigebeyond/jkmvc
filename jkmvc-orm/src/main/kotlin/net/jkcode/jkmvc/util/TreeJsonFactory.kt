package net.jkcode.jkmvc.util

import net.jkcode.jkmvc.orm.IOrm
import net.jkcode.jkmvc.orm.columnIterator
import java.util.*

/**
 * 树节点json的工厂
 *    泛型T是id/pid的类型
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-06-26 17:09:27
 */
class TreeJsonFactory<T>(
        public val idField: String, // 子项id字段名
        public val pidField: String // 子项pid字段名
) {
    /**
     * 构建树形节点
     *
     * @param list 原始的orm列表
     * @param dataBuilder 其他数据构建器
     * @return
     */
    public fun buildTreeJsons(list: List<out IOrm>, dataBuilder: (IOrm) -> MutableMap<String, Any?> = { it -> it.toMap() }): List<TreeJson> {
        // 构建节点哈希
        val nodes = list2map(list, dataBuilder)
        // 构建树形
        val result = ITreeNode.buildTreeNodes(list.columnIterator(idField), nodes)
        nodes.clear()
        return result as List<TreeJson>
    }

    /**
     * orm列表转节点哈希：<id, 节点>
     *
     * @param list orm列表
     * @param dataBuilder 其他数据构建器
     * @return
     */
    protected fun list2map(list: List<out IOrm>, dataBuilder: (IOrm) -> MutableMap<String, Any?>): HashMap<T, TreeJson> {
        val result = HashMap<T, TreeJson>()
        list.forEach {
            val id: T = it[idField]
            val pid: T = it[pidField]
            val data = dataBuilder.invoke(it)
            val value: TreeJson = TreeJson(data)
            result[id] = value
        }
        return result
    }
    
    /**
     * 树形节点json，泛型T是id/pid的类型
     */
    inner class TreeJson(protected val data: MutableMap<String, Any?>) : ITreeNode<T>(), Map<String, Any?> by data {
        /**
         * 子节点
         */
        override val children: MutableList<ITreeNode<T>>
            get() = data.getOrPut("children"){
                LinkedList<ITreeNode<T>>()
            } as MutableList<ITreeNode<T>>

        /**
         * 节点id
         */
        public override val id: T
            get() = data[idField] as T

        /**
         * 父节点id
         */
        public override val pid: T?
            get() = data[pidField] as T

    }
}