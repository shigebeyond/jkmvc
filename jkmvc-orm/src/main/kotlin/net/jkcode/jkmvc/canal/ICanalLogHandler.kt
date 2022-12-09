package net.jkcode.jkmvc.canal

/**
 * canal日志处理器接口
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2022-12-9 7:13 PM
 */
interface ICanalLogHandler {

    /**
     * 过滤表
     */
    fun filter(table: String): Boolean

    /**
     * 处理插入行的日志
     * @param row 插入的行数据
     */
    fun handleInsert(row: Map<String, String?>){
    }

    /**
     * 处理删除行的日志
     * @param row 删除的行数据
     */
    fun handleDelete(row: Map<String, String?>){
    }

    /**
     * 处理更新行的日志
     * @param oldRow 更新前的行数据
     * @param newRow 更新的行数据，只包含变动的列，不包含没变的列
     */
    fun handleUpdate(oldRow: Map<String, String?>, newRow: Map<String, String?>){
    }
}