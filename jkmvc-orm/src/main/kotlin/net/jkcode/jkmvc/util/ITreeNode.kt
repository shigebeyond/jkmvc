package net.jkcode.jkmvc.util

/**
 * 树形节点，泛型T是id/pid的类型
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-06-26 17:09:27
 */
abstract class ITreeNode<T> {

    companion object {

        /**
         * 构建树形节点
         *
         * @param ids 节点id迭代器，用来标识节点顺序
         * @param nodes 节点哈希：<id, 节点>
         * @return
         */
        public fun <K> buildTreeNodes(ids:Iterator<Any?>, nodes: Map<K, ITreeNode<K>>): List<ITreeNode<K>> {
            val result = ArrayList<ITreeNode<K>>()
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
     * 节点id
     */
    public abstract val id: T

    /**
     * 父节点id
     */
    public open val pid: T? = null

    /**
     * 子节点
     */
    public abstract val children: MutableList<ITreeNode<T>>

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
    public fun addChild(child: ITreeNode<T>): ITreeNode<T> {
        children.add(child)
        return this
    }
}