package net.jkcode.jkmvc.util

import net.jkcode.jkmvc.orm.IOrm
import net.jkcode.jkmvc.orm.columnIterator
import java.util.*
import kotlin.collections.ArrayList

/**
 * 树形节点，泛型T是id/pid的类型
 * 以下模型使用
 *  1 台区： 构成3层树形 1 变电站 2 线路 3 台区
 *  2 计划分类： 构成2层树形
 */
data class TreeNode<T>(
        val id:T /* 节点id */,
        val name:String /* 节点名 */,
        val pid:T? = null, /* 父节点id */
        val data: Any? = null, /* 其他数据 */
        val children:MutableList<TreeNode<T>> = ArrayList() /* 子节点 */
){

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
        public fun <K, M: IOrm> buildTreeNodes(list:List<M>, idField:String, pidField:String, nameField:String, dataBuilder: (M)-> Any? = { it -> null }): List<TreeNode<K>> {
            // 构建节点哈希
            val nodes = list2map<K, M>(list, idField, pidField, nameField, dataBuilder)
            // 构建树形
            val result = buildTreeNodes(list.columnIterator(idField), nodes)
            nodes.clear()
            return result
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
        protected fun <K, M: IOrm> list2map(list:List<M>, idField:String, pidField:String, nameField:String, dataBuilder: (M)-> Any?): HashMap<K, TreeNode<K>> {
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

        /**
         * 构建树形节点
         *
         * @param ids 节点id迭代器，用来标识节点顺序
         * @param nodes 节点哈希：<id, 节点>
         * @return
         */
        protected fun <K> buildTreeNodes(ids:Iterator<Any?>, nodes: Map<K, TreeNode<K>>): List<TreeNode<K>> {
            val result = ArrayList<TreeNode<K>>()
            // 按顺序迭代节点
            for(id in ids){
                val node = nodes[id]!!
                if(node.isPidEmpty()){ // 无父节点：收集为根节点
                    result.add(node)
                }else{ // 有父节点：构建父子关系
                    val parent = nodes[node.pid]!!
                    parent.addChild(node)
                }
            }

            return result
        }
    }

    /**
     * 检查父节点id是否为空
     */
    public fun isPidEmpty(): Boolean {
        return pid == null
            || pid is Int && pid == 0 // 0
            || pid is String && pid == "" // 空字符串
    }

    /**
     * 添加子节点
     */
    public fun addChild(child: TreeNode<T>): TreeNode<T> {
        children.add(child)
        return this
    }

    /**
     * 获得最后的子节点
     */
    public fun lastChild(): TreeNode<T>? {
        return children.lastOrNull()
    }
}