package net.jkcode.jkmvc.common

import java.util.*

/**
 * 一致性hash实现
 *
 * @author shijianhang
 * @date 2019-6-20 下午8:02:47
 */
class ConsistentHash<T: Any>(realNodes: Collection<T>, // 真实节点
                            public val virtualNodeMultiple: Int,// 虚拟节点倍数，即一个真实节点对应创建 virtualNodeMultiple 个虚拟节点
                            public val virtualNodeMaxSize: Int = 360, // 虚拟节点最大个数
                            public val hashFunction: (Any) -> Int = { it.hashCode() } // 哈希函数, 对node/key取哈希, node(节点如机器)与key(主体如缓存键)的类型不同
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
    protected fun forEachVirtualIndexFromRealNode(realNode: T, action: (Int) -> Unit){
        val hash = hashFunction.invoke(realNode)
        for (i in 0 until virtualNodeMultiple) {
            val index = (hash + virtualIndexSpan * i) % virtualNodeMaxSize
            action.invoke(index)
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
        if (virtualNodes.isEmpty())
            return null

        // 1 计算key对应的hash
        val hash = hashFunction.invoke(key)
        val i = hash % virtualNodeMaxSize

        // 2 根据hash查找节点
        // 2.1 命中节点, 直接返回该节点
        if(virtualNodes.containsKey(i))
            return virtualNodes[i]

        // 2.2 没有命中节点, 则顺时针方向查找下一个节点
        val entry = virtualNodes.ceilingEntry(i)
        if (entry != null)
            return entry.value

        // 2.3 没有下一个节点, 则返回第一个节点
        return virtualNodes.firstEntry().value
    }
}