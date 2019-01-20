package com.jkmvc.http.util

import com.jkmvc.common.Config
import com.jkmvc.http.HttpRequest

/**
 * 分页处理，主要用于数据库分页
 *
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-09-21 4:40 PM
 */
open class Pagination(
        public override val total:Int /* 记录总数 */,
        protected val reqPage:Int = getRequestPage() /* 请求页码 */,
        public override val pageSize:Int = config["pageSize"]!! /* 每页的记录数 */
) : IPagination {

    companion object {
        /**
         * 分页配置
         */
        val config: Config = Config.instance("pagination");

        /**
         * 获得页码的请求参数
         */
        public fun getRequestPage():Int{
            return HttpRequest.current().get(config["pageParameterName"]!!, 1)!! // 默认为第一页
        }
    }

    /**
     * 页码总数
     */
    public override val totalPages:Int = maxOf((total + pageSize - 1) / pageSize, 1) // 最少1页

    /**
     * 当前页码
     */
    public override val page:Int = minOf(totalPages, maxOf(reqPage, 1))

    /**
     * 开始位置
     */
    public override val startIndex:Int = pageSize * (page - 1)

    /**
     * 结束位置
     */
    public override val endIndex:Int = minOf(total, pageSize * page)
}