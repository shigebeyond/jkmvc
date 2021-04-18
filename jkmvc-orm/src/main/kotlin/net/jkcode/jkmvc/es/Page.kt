package net.jkcode.jkmvc.es

import java.io.Serializable
import java.util.ArrayList

class Page<T> : Serializable {

    private var pageSize: Int = 0
    private var last: Int = 0

    private var first: Int = 0
    private var next: Int = 0
    private var prev: Int = 0
    var pageNo: Int = 0
    private var centerNum: Int = 0
    private var orderBy: String? = null
    private var list: List<T>? = null
    private var count: Long = 0

    val firstResult: Int
        get() {
            var a = (this.pageNo - 1) * this.getPageSize()
            if (this.getCount() != -1L && a.toLong() >= this.getCount()) {
                a = 0
            }

            return a
        }

    val maxResults: Int
        get() = this.getPageSize()

    val isNotPaging: Boolean
        get() = this.pageSize == -1

    val isOnlyCount: Boolean
        get() = this.count == -2L

    val isNotCount: Boolean
        get() = this.count == -1L || this.isNotPaging

    fun getList(): List<T>? {
        return this.list
    }

    fun getPageSize(): Int {
        return this.pageSize
    }

    fun setOrderBy(orderBy: String) {
        this.orderBy = orderBy
    }


    @JvmOverloads
    constructor(pageNo: Int, pageSize: Int, count: Long = 0L, list: List<T>? = null) {
        this.pageNo = 1
        this.pageSize = pageSize
        this.list = list
        this.centerNum = 5

        this.pageNo = pageNo
        this.pageSize = pageSize
        this.count = count
        if (list != null) {
            this.list = list
        }
    }

    fun getCount(): Long {
        return this.count
    }

    fun setPageSize(pageSize: Int) {
        if (pageSize <= 0) {
            this.pageSize = 20
        } else {
            this.pageSize = pageSize
        }
    }

    fun initialize() {
        if (!this.isNotPaging && !this.isNotCount && !this.isOnlyCount) {
            if (this.pageSize <= 0) {
                this.pageSize = 20
            }

            this.first = 1
            this.last = (this.count / this.pageSize.toLong()).toInt() + this.first - 1
            if (this.count % this.pageSize.toLong() != 0L || this.last == 0) {
                ++this.last
            }

            if (this.last < this.first) {
                this.last = this.first
            }

            if (this.pageNo <= this.first) {
                this.pageNo = this.first
            }

            if (this.pageNo >= this.last) {
                this.pageNo = this.last
            }

            val var10000: Page<*>
            if (this.pageNo > 1) {
                var10000 = this
                this.prev = this.pageNo - 1
            } else {
                var10000 = this
                this.prev = this.first
            }

            if (var10000.pageNo < this.last - 1) {
                this.next = this.pageNo + 1
            } else {
                this.next = this.last
            }
        }
    }

    constructor() {
        this.pageNo = 1
        this.pageSize = 10
        this.list = ArrayList()
        this.centerNum = 5
    }

    fun setCount(count: Long) {
        if ((this.count = count) != -1L && this.pageSize.toLong() >= count) {
            this.pageNo = 1
        }

    }

    fun setList(list: List<T>?): Page<T> {
        var list = list
        if (list == null) {
            list = ArrayList()
        }
        this.list = list
        return this
    }

    fun setCenterNum(centerNum: Int) {
        this.centerNum = centerNum
    }

    companion object {

        val COUNT_NOT_COUNT = -1
        val PAGE_SIZE_NOT_PAGING = -1
        private const val serialVersionUID = 1L
        val COUNT_ONLY_COUNT = -2
    }

}