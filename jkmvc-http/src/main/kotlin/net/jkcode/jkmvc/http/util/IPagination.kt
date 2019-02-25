package net.jkcode.jkmvc.http.util

/**
 * 分页处理
 *
 * @ClassName: IPagination
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-09-21 5:08 PM
 */
interface IPagination {
    /**
     * 每页的记录数
     */
    val pageSize:Int;

    /**
     * 当前页码
     */
    val page:Int;

    /**
     * 记录总数
     */
    val total:Int;

    /**
     * 页码总数
     */
    val totalPages:Int;

    /**
     * 开始位置
     */
    val startIndex:Int;

    /**
     * 结束位置
     */
    val endIndex:Int;
}