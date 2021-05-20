package net.jkcode.jkmvc.es

import io.searchbox.client.JestResult
import io.searchbox.core.SearchResult
import java.io.Closeable
import java.util.AbstractCollection
import java.util.NoSuchElementException
import java.util.concurrent.atomic.AtomicInteger

/**
 * 有游标的结果集合, 在迭代中查询下一页
 *   只能迭代一次, 迭代一次后会close()
 *   由于es scroll搜索对分页无效, 则EsScrollCollection不是分页数据, 而是全部数据, 则 size 是全部行数
 */
class EsScrollCollection<T>(
        protected val esMgr: EsManager,
        protected val result: SearchResult, // 首次搜索的结果
        protected val scrollTimeInMillis: Long, // 游标的有效时间, 如果报错`Elasticsearch No search context found for id`, 则加大
        protected val resultMapper:(JestResult)->Collection<T> // 结果转换器, 会将每一页的JestResult(兼容SearchResult/ScrollSearchResult), 转为T对象集合
) : AbstractCollection<T>() {

    // 迭代次数
    protected var iterateCount = AtomicInteger(0);

    // 总数
    override val size: Int = result.total.toInt()

    // 获得迭代器
    override fun iterator(): MutableIterator<T> {
        if(iterateCount.getAndIncrement() > 0)
            throw EsException("EsScrollCollection 只能迭代一次")

        return ScrollIterator(result)
    }

    /**
     * 有游标的迭代器
     *   只能迭代一次, 迭代一次后会close()
     */
    inner class ScrollIterator(protected var currResult: JestResult) : MutableIterator<T>, Closeable {

        // 当前结果hit, 每天切换结果会改变
        protected var currHits: Iterator<T>? = null

        // 游标id, 每天切换结果会改变
        protected var scrollId: String? = null

        // 迭代完成, 每天切换结果会改变
        private var finished = true

        init {
            onToggleResult()
        }

        /**
         * 切换下一页的结果的处理
         */
        protected fun onToggleResult() {
            currHits = resultMapper(currResult!!).iterator()
            finished = !currHits!!.hasNext()
            scrollId = currResult.scrollId
            if (finished)
                close()
        }

        override fun close() {
            try {
                // 虽然es 会有自动清理机制，但是 scroll_id 的存在会耗费大量的资源来保存一份当前查询结果集映像，并且会占用文件描述符。所以用完之后要及时清理
                if (scrollId != null)
                    esMgr.clearScroll(scrollId!!)
            } finally {
                currHits = null
                scrollId = null
            }
        }

        override operator fun hasNext(): Boolean {
            // 检查是否结束
            if (finished)
                return false

            // 当前页迭代完, 就查询下一页
            if (currHits != null && !currHits!!.hasNext()) {
                // 查询下一页
                currResult = esMgr.continueScroll(scrollId!!, scrollTimeInMillis)
                // Save hits and scroll id
                onToggleResult()
            }

            // 迭代
            return currHits?.hasNext() ?: false
        }

        override operator fun next(): T {
            if (hasNext())
                return currHits!!.next()

            throw NoSuchElementException()
        }

        override fun remove() {
            throw UnsupportedOperationException("remove")
        }
    }

}