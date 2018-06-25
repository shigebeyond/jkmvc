package com.jkmvc.http.util

/**
 * 全量数据的分页处理，主要用于内存分页
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-09-21 4:40 PM
 */
class AllPagination<T>(
        public val items:List<T>, /* 全量数据 */
        reqPage:Int = getRequestPage() /* 当前页码 */,
        pageSize:Int = config["pageSize"]!! /* 每页的记录数 */
) : Pagination(items.size, reqPage, pageSize) {

    /**
     * 当前页的数据
     */
    public val pageItems:List<T> by lazy{
        items.subList(startIndex, endIndex)
    }
}