package net.jkcode.jkmvc.canal

import net.jkcode.jkmvc.orm.Orm
import net.jkcode.jkutil.common.getMethodByName
import net.jkcode.jkutil.common.isSubClass
import net.jkcode.jkutil.common.ucFirst

/**
 * canal日志处理器
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2022-12-9 7:13 PM
 */
abstract class CanalLogHandler(
    public val schema: String, // 库名, 支持*代表任意几个字符
    public val table: String // 表名, 支持*代表任意几个字符
) : ICanalLogHandler {

    /**
     * 表正则
     */
    protected lateinit var tableReg: Regex

    init {
        val s = schema.replace("*", ".*")
        val t = table.replace("*", ".*")
        tableReg = "^$s\\.$t$".toRegex()
    }

    /**
     * 能处理的事件
     *   就是在子类中重写了事件处理函数
     */
    internal val processableEvents: List<String> by lazy {
        "insert|delete|update".split('|').filter { event ->
            val methodName = "handle" + event.ucFirst() // 获得handleXXX()方法
            val method = this.javaClass.getMethodByName(methodName)!!
            method.declaringClass.isSubClass(CanalLogHandler::class.java)
        }
    }


    /**
     * 过滤表
     */
    override fun filter(table: String): Boolean {
        return tableReg.matches(table)
    }
}