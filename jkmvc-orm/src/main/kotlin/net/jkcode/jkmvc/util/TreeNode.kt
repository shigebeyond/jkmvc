package net.jkcode.jkmvc.util

import net.jkcode.jkmvc.orm.IOrm
import net.jkcode.jkmvc.orm.columnIterator
import java.util.*

/**
 * 树形节点，泛型T是id/pid的类型
 * 以下模型使用
 *  1 台区： 构成3层树形 1 变电站 2 线路 3 台区
 *  2 计划分类： 构成2层树形
 */
data class TreeNode<T>(
        override val id:T /* 节点id */,
        val name:String /* 节点名 */,
        override val pid:T? = null, /* 父节点id */
        val data: Any? = null, /* 其他数据 */
        override val children: MutableList<ITreeNode<T>> = LinkedList()
) : ITreeNode<T>() {

    companion object{

        /**
         * 构建树形节点
         *
         * @param list 原始的orm列表
         * @param idField 子项id字段名，其值作为结果哈希的key
         * @param pidField 子项pid字段名，与其他两个字段组成节点，作为作为结果哈希的value
         * @param nameField 子项name字段名，与其他两个字段组成节点，作为作为结果哈希的value
         * @param dataBuilder 其他数据构建器
         * @return
         */
        public fun <K> buildTreeNodes(list:List<out IOrm>, idField:String, pidField:String, nameField:String, dataBuilder: (IOrm)-> Any? = { it -> null }): List<TreeNode<K>> {
            // 构建节点哈希
            val nodes = list2map<K>(list, idField, pidField, nameField, dataBuilder)
            // 构建树形
            val result = buildTreeNodes(list.columnIterator(idField), nodes)
            nodes.clear()
            return result as List<TreeNode<K>>
        }

        /**
         * orm列表转节点哈希：<id, 节点>
         *
         * @param list orm列表
         * @param idField 子项id字段名，其值作为结果哈希的key
         * @param pidField 子项pid字段名，与其他两个字段组成节点，作为作为结果哈希的value
         * @param nameField 子项name字段名，与其他两个字段组成节点，作为作为结果哈希的value
         * @param dataBuilder 其他数据构建器
         * @return
         */
        protected fun <K> list2map(list:List<out IOrm>, idField:String, pidField:String, nameField:String, dataBuilder: (IOrm)-> Any?): HashMap<K, TreeNode<K>> {
            val result = HashMap<K, TreeNode<K>>()
            list.forEach {
                val id:K = it[idField]
                val pid:K = it[pidField]
                //val name:String = it[nameField]
                val name:String = it.compileTemplate(nameField) // 编译字符串模板
                val data = dataBuilder.invoke(it)
                val value: TreeNode<K> = TreeNode(id, name, pid, data)
                result[id] = value
            }
            return result
        }

    }

}