package net.jkcode.jkmvc.common

/**
 * 集合的包装器
 *    包含函数为 fun decorateCollection(col: Collection<T>, transform: (T) -> R): Collection<R>
 *    跟 fun Iterable<T>.map(transform: (T) -> R): List<R> 功能是类似的, 只不过他不创建 ArrayList 对象来保存元素, 即不保存元素, 只是引用代理
 *    使用:
 *    1 如果集合元素很多, AbstractCollectionDecorator 优于 Iterable<T>.map()
 *    2 如果转换操作代价很大, Iterable<T>.map() 优于 AbstractCollectionDecorator
 *    3 如果需要序列化, Iterable<T>.map() 优于 AbstractCollectionDecorator, 免得序列化一些不必要的东西
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-22 4:24 PM
 */
class CollectionDecorator<E, R>(
        protected val col: Collection<E>, // 被代理的集合
        protected val transform: (E) -> R // 转换函数
): Collection<R> by (col as Collection<R>) {

    /**
     * 判断是否包含单个元素
     */
    public override fun contains(element: R): Boolean {
        return col.any {
            transform(it) == element
        }
    }

    /**
     * 判断是否包含多个元素
     */
    public override fun containsAll(elements: Collection<R>): Boolean {
        return elements.all { contains(it) }
    }

    /**
     * 获得迭代器
     */
    public override fun iterator(): Iterator<R> {
        return decorateIterator(col.iterator(), transform)
    }

    public override fun toString(): String{
        return joinToString(", ", "[", "]")
    }

}