package net.jkcode.jkmvc.common

import java.util.*

/**
 * 一致性hash实现
 *
 * @author shijianhang
 * @date 2019-6-20 下午8:02:47
 */
class ConsistentHash<T: Any>(public val virtualNodeMultiple: Int,// 虚拟节点倍数，即一个真实节点对应创建 virtualNodeMultiple 个虚拟节点
                             public val virtualNodeMaxSize: Int = 100, // 虚拟节点最大个数
                             realNodes: Collection<T> = emptyList(), // 真实节点
                             public val hashFunc: (Any) -> Int = { it.hashCode() /* int溢出可能是负数 */ and Integer.MAX_VALUE } // 哈希函数, 对node/key取哈希, node(节点如机器)与key(主体如缓存键)的类型不同
) {
    /**
     * 虚拟节点序号的间隔
     */
    protected val virtualIndexSpan: Int = virtualNodeMaxSize / virtualNodeMultiple

    /**
     * <虚拟节点的hash值, 真实节点>
      */
    protected val virtualNodes = TreeMap<Int, T>()

    /**
     * 虚拟节点个数
     */
    public val size: Int
        get() = virtualNodes.size

    init {
        // 添加真实节点
        for (node in realNodes)
            add(node)
    }

    /**
     * 输出虚拟节点
     */
    public fun dumpVirtualNodes(){
        println(virtualNodes)
    }

    /**
     * 遍历真实节点对应的每个虚拟节点的序号
     *     一个真实节点对应创建 virtualNodeMultiple 个虚拟节点
     *
     * @param realNode 真实节点
     * @param action 操作
     */
    protected inline fun forEachVirtualIndexFromRealNode(realNode: T, action: (Int) -> Unit){
        val hash = hashFunc.invoke(realNode)
        var index = hash % virtualNodeMaxSize
        for (i in 0 until virtualNodeMultiple) {
            action.invoke(index)
            index = (index + virtualIndexSpan) % virtualNodeMaxSize
        }
    }

    /**
     * 添加真实节点
     *    添加对应虚拟节点
     * @param realNode 真实节点
     */
    public fun add(realNode: T) {
        forEachVirtualIndexFromRealNode(realNode){ i ->
            virtualNodes[i] = realNode
        }
    }

    /**
     * 删除真实节点
     *    删除对应虚拟节点
     * @param realNode
     */
    public fun remove(realNode: T) {
        forEachVirtualIndexFromRealNode(realNode){ i ->
            // 有可能hash碰撞, 因此先检查是否该真实节点
            if(virtualNodes[i] == realNode)
                virtualNodes.remove(i)
        }
    }

    /**
     * 根据key, 计算hash, 获得离hash最近的顺时针节点
     *
     * @param key
     * @return
     */
    public operator fun get(key: Any): T? {
        // 计算key对应的hash
        val hash = hashFunc.invoke(key)
        val index = hash % virtualNodeMaxSize
        return get(index)
    }

    /**
     * 获得离i最近的顺时针节点
     *
     * @param index 节点序号
     * @return
     */
    public operator fun get(index: Int): T? {
        if (virtualNodes.isEmpty())
            return null

        // 根据hash查找节点
        // 1 命中节点, 直接返回该节点
        if(virtualNodes.containsKey(index))
            return virtualNodes[index]

        // 2 没有命中节点, 则顺时针方向顺序查找下一个节点
        val entry = virtualNodes.ceilingEntry(index)
        if (entry != null)
            return entry.value

        // 3 没有下一个节点, 则返回第一个节点
        return virtualNodes.firstEntry().value
    }
}