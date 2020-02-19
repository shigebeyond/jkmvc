package net.jkcode.jkmvc.tags.form

import net.jkcode.jkutil.common.PropertyHandler
import net.jkcode.jkutil.common.trim
import javax.servlet.http.HttpServletRequest
import javax.servlet.jsp.tagext.TagSupport

/**
 * 绑定值的标签
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-25 3:35 PM
 */
abstract class BaseBoundTag: TagSupport() {

    /**
     * 会话
     */
    protected val session
        get() = pageContext.session

    /**
     * 请求
     */
    protected val request
        get() = pageContext.request as HttpServletRequest

    /**
     * 属性路径
     */
    public var path: String? = null

    /**
     * 属性的绝对路径
     *   暂时只支持2层
     */
    public val absolutePath: String? by lazy{
        if(path == null)
            null
        else { // 2层全路径
            // 子路径
            var absolutePath = path!!
            // 父路径
            val parent = findAncestorWithClass(this, BaseBoundTag::class.java) as BaseBoundTag?
            if (parent?.path != null)
                absolutePath = "${parent.path}.$absolutePath"
            absolutePath
        }
    }

    /**
     * 绑定值
     */
    public val boundValue: Any? by lazy{
        if(absolutePath == null)
            null
        else {
            val keys = absolutePath!!.split("\\.".toRegex(), 2)
            var value = request.getAttribute(keys[0])
            if (keys.size == 2)
                value = PropertyHandler.getPath(value, keys[1])
            value
        }
    }

    /**
     * 绑定值类型
     */
    public val boundType: Class<*>? by lazy{
        if(absolutePath == null)
            null
        else {
            val keys = absolutePath!!.split("\\.".toRegex(), 2)
            var value = request.getAttribute(keys[0])
            if (keys.size == 2)
                PropertyHandler.getPathType(value, keys[1])
            else
                value.javaClass
        }
    }

    /**
     * 绑定的错误
     */
    public val boundError: Any? by lazy{
        if(absolutePath == null)
            null
        else {
            val errors = request.getAttribute("_errors")
            val path = if (absolutePath!!.endsWith(".*")) // 所有错误
                            absolutePath!!.trim(".*")
                        else // 单个错误
                            absolutePath!!
            PropertyHandler.getPath(errors, path)
        }
    }

    /**
     * 检查绑定值是否等于指定值
     */
    public fun isBoundValueEquals(value: Any?): Boolean {
        return value != null && boundValue != null && boundValue!!.equals(value)
    }

    /**
     * 是否有错
     */
    public val isError: Boolean
        get(){
            return !(boundError == null || boundError is Map<*, *> && (boundError as Map<*, *>).isEmpty()) // 不为空
        }
}